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

import ac.adproj.mchat.handler.ClientMessageHandler;
import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.service.CommonThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Listener class of Client.
 *
 * @author Andy Cheung
 */
@Slf4j
public class ClientListener implements Listener {
    private final AtomicInteger threadNumberOfScheduledThread = new AtomicInteger(0);

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2,
            r -> new Thread(r, "Scheduled Thread Pooling Thread - " +
                    threadNumberOfScheduledThread.getAndIncrement()));
    private final String name;
    private AsynchronousSocketChannel socketChannel;
    private String uuid;
    private ScheduledFuture<?> scheduledFutureOfKeepAliveSender;

    public ClientListener(Shell shell, Consumer<String> uiActions, byte[] address, int port, String username)
            throws IOException {
        this.name = username;
        init(shell, uiActions, address, port, username);
    }

    /**
     * Check username duplication asynchronously.
     *
     * @param serverAddress The server address.
     * @param name The username.
     * @param completionHandler Handler of successful situations.
     *                          If the handler argument is true,
     *                          the username entered is reserved by others.
     *
     * @param failureHandler Handler of failed situations.
     */
    public static void checkNameDuplicatesAsync(byte[] serverAddress,
                                                String name, Consumer<Boolean> completionHandler,
                                                Runnable failureHandler) {
        CommonThreadPool.execute(() -> {
            ReentrantLock lock = new ReentrantLock(true);
            AtomicInteger result = new AtomicInteger(-1);
            AtomicReference<Thread> checkerThread = new AtomicReference<>(null);

            CountDownLatch latch = new CountDownLatch(1);

            // Run the actual checker logic in another thread.

            CommonThreadPool.execute(() -> doUsernameCheckAsync(serverAddress, name, lock, result, checkerThread, latch));

            try {
                // Block the thread within specified seconds.

                boolean isCheckerThreadFinished = latch.await(5, TimeUnit.SECONDS) && lock.tryLock();

                if (!isCheckerThreadFinished || result.get() == -1) {
                    // Interrupt the checker thread.
                    checkerThread.get().interrupt();

                    // Timeout exceeded.
                    failureHandler.run();

                    return;
                }

                completionHandler.accept(result.get() == 1);

            } catch (InterruptedException e) {
                e.printStackTrace();

                Thread.currentThread().interrupt();

                if (result.get() == -1) {
                    failureHandler.run();
                }
            }
        });
    }

    private static void doUsernameCheckAsync(byte[] serverAddress, String name,
                                             ReentrantLock lock, AtomicInteger result,
                                             AtomicReference<Thread> checkerThread, CountDownLatch latch) {
        checkerThread.set(Thread.currentThread());

        lock.lock();
        try {
            result.compareAndSet(-1, checkNameDuplicates(serverAddress, name) ? 1 : 0);
        } catch (ClosedByInterruptException ignored) {
            // Timeout exceeded.
            // Ignore the exception.

            result.set(-1);

        } catch (IOException e) {
            log.error("I/O exception when checking duplicate.", e);
            result.set(-1);

        } finally {
            lock.unlock();
            latch.countDown();
        }
    }

    public static boolean checkNameDuplicates(byte[] serverAddress, String name) throws IOException {
        DatagramChannel dc = DatagramChannel.open();

        ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);

        bb.put((Protocol.CHECK_DUPLICATE_REQUEST_HEADER + name).getBytes(StandardCharsets.UTF_8));
        bb.flip();

        StringBuilder buffer = new StringBuilder();

        try {
            dc.configureBlocking(true);
            dc.send(bb, new InetSocketAddress(InetAddress.getByAddress(serverAddress), Protocol.SERVER_CHECK_DUPLICATE_PORT));

            bb.clear();
            dc.receive(bb);
            bb.flip();

            while (bb.hasRemaining()) {
                buffer.append(StandardCharsets.UTF_8.decode(bb));
            }

            return !buffer.toString().startsWith(Protocol.USER_NAME_NOT_EXIST);

        } catch (ClosedByInterruptException e) {

            // Interrupted by parent thread.
            // Propagate the situation to the parent thread.

            throw e;

        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error when performing I/O operation.", e);

            throw new RuntimeException(e);
        } finally {
            dc.close();
        }
    }

    private void registerKeepaliveSender() {
        scheduledFutureOfKeepAliveSender = scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> {
            if (log.isDebugEnabled()) {
                log.debug("Sending KA message...");
            }

            sendCommunicationData(Protocol.KEEP_ALIVE_HEADER + uuid + Protocol.KEEP_ALIVE_TAIL, "");

        }, 0, 10, TimeUnit.SECONDS);
    }

    private void init(Shell shell, Consumer<String> uiActions, byte[] address, int port, String username)
            throws IOException {
        socketChannel = AsynchronousSocketChannel.open();

        socketChannel.bind(new InetSocketAddress(port));

        InetAddress ia = InetAddress.getByAddress(address);

        uuid = UUID.randomUUID().toString();

        initNioSocketConnection(shell, uiActions, ia, username);

        registerKeepaliveSender();
    }

    private void initNioSocketConnection(Shell shell, Consumer<String> uiActions, InetAddress ia, String username) {
        ClientMessageHandler handler = new ClientMessageHandler(v -> {
            try {
                disconnectWithoutNotification();
            } catch (IOException e) {
                e.printStackTrace();
                shell.getDisplay().syncExec(() -> MessageDialog.openError(shell, "出错", "下线出错：" + e.getMessage()));
            }
        });

        socketChannel.connect(new InetSocketAddress(ia, SERVER_PORT), uuid, new CompletionHandler<>() {

            @Override
            public void completed(Void result, String attachment) {
                sendGreetingMessage(attachment, username, shell, uiActions);

                final ByteBuffer buffer = ByteBuffer.allocate(Protocol.BUFFER_SIZE);

                socketChannel.read(buffer, shell.getDisplay(), new CompletionHandler<>() {

                    @Override
                    public void completed(Integer result, Display display) {
                        if (result != -1) {
                            buffer.flip();

                            StringBuilder sbuffer = new StringBuilder();

                            while (buffer.hasRemaining()) {
                                sbuffer.append(StandardCharsets.UTF_8.decode(buffer));
                            }

                            display.syncExec(
                                    () -> uiActions.accept(handler.handleMessage(sbuffer.toString(), socketChannel)));

                            buffer.clear();
                        }

                        if (socketChannel != null && socketChannel.isOpen()) {
                            socketChannel.read(buffer, shell.getDisplay(), this);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Display display) {
                        if (exc.getClass() == AsynchronousCloseException.class) {
                            return;
                        }

                        exc.printStackTrace();

                        display.syncExec(() -> {
                            MessageDialog.openError(shell, "出错", "客户机读取出错：" + exc.getMessage());
                        });
                    }
                });
            }

            @Override
            public void failed(Throwable exc, String attachment) {
                exc.printStackTrace();

                if (shell != null && !shell.isDisposed()) {
                    shell.getDisplay().syncExec(() -> {
                        MessageDialog.openError(shell, "出错", "客户机读取出错：" + exc.getMessage());
                    });
                }
            }
        });
    }

    private void sendGreetingMessage(String attachment, String username, Shell shell, Consumer<String> uiActions) {
        String greetMessage = CONNECTING_GREET_LEFT_HALF + attachment + CONNECTING_GREET_MIDDLE_HALF + username;
        final ByteBuffer greetBuffer = ByteBuffer.wrap(greetMessage.getBytes());

        try {
            socketChannel.write(greetBuffer).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        shell.getDisplay().syncExec(() ->
                uiActions.accept(username + "，欢迎来到聊天室。"));
    }

    public String getUuid() {
        return uuid;
    }

    public String getUserName() {
        return name;
    }

    @Override
    public boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen();
    }

    @Override
    public void sendCommunicationData(String text, String uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        try {
            socketChannel.write(bb).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        sendMessage(message, uuid);
    }

    @Override
    public void sendMessage(String message, String uuidValue) {
        sendCommunicationData(
                MESSAGE_HEADER_LEFT_HALF + uuidValue + MESSAGE_HEADER_MIDDLE_HALF + MESSAGE_HEADER_RIGHT_HALF + message,
                uuidValue);
    }

    public void disconnect() throws IOException {
        if (isConnected()) {
            sendCommunicationData(DISCONNECT + uuid, uuid);
            disconnectWithoutNotification();
        }
    }

    public void disconnectWithoutNotification() throws IOException {
        if (isConnected()) {
            socketChannel.close();
            socketChannel = null;
        }

        scheduledFutureOfKeepAliveSender.cancel(false);
        scheduledThreadPoolExecutor.shutdownNow();
        CommonThreadPool.shutdown();
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}
