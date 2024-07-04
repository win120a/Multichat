/*
    Copyright (C) 2011-2024 Andy Cheung

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ac.adproj.mchat.protocol;

import ac.adproj.mchat.handler.Handler;
import ac.adproj.mchat.handler.ServerMessageHandler;
import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.model.User;
import ac.adproj.mchat.service.HeartbeatDetectingService;
import ac.adproj.mchat.service.MessageDistributor;
import ac.adproj.mchat.service.UserManager;
import ac.adproj.mchat.service.UserNameQueryService;
import ac.adproj.mchat.ui.CommonDialogs;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener class of Chatting Server.
 *
 * @author Andy Cheung
 */
@Slf4j
public class ServerListener implements Listener {

    private static ServerListener instance;
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private ExecutorService threadPool;
    private UserNameQueryService usernameQueryService;
    private AtomicInteger threadNumber = new AtomicInteger(0);

    private final AtomicInteger threadNumberOfScheduledThread = new AtomicInteger(0);

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2,
            r -> new Thread(r, "Scheduled Thread Pooling Thread - " +
                    threadNumberOfScheduledThread.getAndIncrement()));

    private ScheduledFuture scheduledFutureOfHeartbeatDetectingService;

    private ServerListener() throws IOException {
        init();
    }

    /**
     * Obtain the only instance.
     *
     * @return The only instance.
     * @throws IOException If I/O Error occurs.
     */
    public static ServerListener getInstance() throws IOException {
        if (instance == null) {
            instance = new ServerListener();
        }

        return instance;
    }

    private void registerHeartbeatDetectingService() {

        scheduledFutureOfHeartbeatDetectingService = scheduledThreadPoolExecutor
                .scheduleWithFixedDelay(new HeartbeatDetectingService(this),
                        0, 30, TimeUnit.SECONDS);
    }

    private void readMessage(ByteBuffer bb, Handler handler, Integer result, AsynchronousSocketChannel channel) {

        if (result != -1) {

            bb.flip();

            StringBuilder messageStringBuilder = new StringBuilder();

            while (bb.hasRemaining()) {
                messageStringBuilder.append(StandardCharsets.UTF_8.decode(bb));
            }

            String message = handler.handleMessage(messageStringBuilder.toString(), channel);

            try {
                MessageDistributor.getInstance().sendUiMessage(message);
            } catch (InterruptedException e) {
                e.printStackTrace();

                try {
                    close();
                } catch (Exception e1) {
                    // ignore
                }
            }

            bb.clear();
        }
    }

    private void init() throws IOException {
        ServerMessageHandler handler = new ServerMessageHandler(this);

        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>(16);

        ThreadFactory threadFactory = r -> {
            if (!r.getClass().getName().contains("DuplicateCheckerService")) {
                return new Thread(r, "PoolThread - SrvListener - " + threadNumber.incrementAndGet());
            } else {
                return new Thread(r, "PoolThread - DCS - " + threadNumber.incrementAndGet());
            }
        };

        threadPool = new ThreadPoolExecutor(4, 16, 2, TimeUnit.MINUTES, bq, threadFactory);

        usernameQueryService = new UserNameQueryService();
        threadPool.submit(usernameQueryService);

        AsynchronousChannelGroup acg = AsynchronousChannelGroup.withThreadPool(threadPool);

        serverSocketChannel = AsynchronousServerSocketChannel.open(acg);
        serverSocketChannel.bind(new InetSocketAddress(Protocol.SERVER_PORT));
        serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                final ByteBuffer bb = ByteBuffer.allocate(Protocol.BUFFER_SIZE);

                /* Handle messages. */
                result.read(bb, result, new CompletionHandler<>() {

                    @Override
                    public void completed(Integer result, AsynchronousSocketChannel channel) {
                        readMessage(bb, handler, result, channel);

                        if (channel.isOpen()) {
                            channel.read(bb, channel, this);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                        log.error("Error when receiving message.", exc);

                        SoftReference<User> sr = null;

                        for (User user : UserManager.getInstance().userProfileValueSet()) {
                            if (user.getChannel().equals(channel)) {
                                sr = new SoftReference<>(user);
                            }
                        }

                        if (sr != null) {
                            UserManager.getInstance().deleteUserProfile(sr.get().getUuid());

                            sr.clear();
                            sr = null;
                        }
                    }
                });

                serverSocketChannel.accept(ServerListener.this, this);
            }

            @Override
            public void failed(Throwable exc, Object o) {
                log.error("Error when accepting socket connection.", exc);
            }
        });

        registerHeartbeatDetectingService();
    }

    @Override
    public boolean isConnected() {
        return !UserManager.getInstance().isEmptyUserProfile();
    }

    @Override
    public void sendCommunicationData(String text, String uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        if (uuid.equals(Protocol.BROADCAST_MESSAGE_UUID)) {
            for (User u : UserManager.getInstance().userProfileValueSet()) {
                try {
                    bb.rewind();

                    while (bb.hasRemaining()) {
                        u.getChannel().write(bb).get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Got error when sending message.", e);
                    CommonDialogs.errorDialog("发送信息出错: " + e.getMessage());
                }
            }
        } else {
            try {
                UserManager.getInstance().lookup(uuid).getChannel().write(bb).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Got error when sending message.", e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendMessage(String message, String uuid) {
        sendCommunicationData(MESSAGE_HEADER_LEFT_HALF + Protocol.BROADCAST_MESSAGE_UUID + MESSAGE_HEADER_MIDDLE_HALF
                + MESSAGE_HEADER_RIGHT_HALF + message, uuid);
    }

    public void disconnect(String uuid) throws IOException {
        UserManager.getInstance().lookup(uuid).getChannel().close();
        UserManager.getInstance().deleteUserProfile(uuid);
    }

    public void disconnectAll() {
        UserManager.getInstance().userProfileValueSet().forEach(value -> {
            try {
                final ByteBuffer bb = ByteBuffer
                        .wrap((Protocol.DISCONNECT + "SERVER").getBytes(StandardCharsets.UTF_8));

                value.getChannel().write(bb).get();
                value.getChannel().close();
            } catch (IOException | InterruptedException | ExecutionException e) {
                // Ignore
            }
        });

        UserManager.getInstance().clearAllProfiles();
    }

    @Override
    public void close() throws Exception {
        usernameQueryService.stopSelf();
        threadPool.shutdownNow();
        UserManager.getInstance().clearAllProfiles();
        serverSocketChannel.close();

        scheduledFutureOfHeartbeatDetectingService.cancel(false);
    }
}
