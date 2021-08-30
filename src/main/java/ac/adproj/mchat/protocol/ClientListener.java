/*
    Copyright (C) 2011-2020 Andy Cheung

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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Listener class of Client.
 *
 * @author Andy Cheung
 */
@Slf4j
public class ClientListener implements Listener {
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
    private AsynchronousSocketChannel socketChannel;
    private String uuid;
    private final String name;
    private ScheduledFuture<?> scheduledFutureOfKeepAliveSender;

    public ClientListener(Shell shell, Consumer<String> uiActions, byte[] address, int port, String username)
            throws IOException {
        this.name = username;
        init(shell, uiActions, address, port, username);
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

        } catch (IOException e) {
            e.printStackTrace();
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

        }, 0, 3, TimeUnit.SECONDS);
    }

    private void init(Shell shell, Consumer<String> uiActions, byte[] address, int port, String username)
            throws IOException {
        socketChannel = AsynchronousSocketChannel.open();

        socketChannel.bind(new InetSocketAddress(port));

        InetAddress ia = InetAddress.getByAddress(address);

        uuid = UUID.randomUUID().toString();

        initNIOSocketConnection(shell, uiActions, ia, username);

        registerKeepaliveSender();
    }

    private void initNIOSocketConnection(Shell shell, Consumer<String> uiActions, InetAddress ia, String username) {
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
                        if (exc.getClass().getName().contains("AsynchronousCloseException")) {
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
                uiActions.accept("Connected to Server, UserName: " + username + ", UUID: " + uuid));
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
            scheduledFutureOfKeepAliveSender.cancel(false);
            scheduledThreadPoolExecutor.shutdownNow();
        }
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}
