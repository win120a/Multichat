package ac.jcourse.mchat.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ac.jcourse.mchat.protocol.handler.Handler;

public class ClientListener implements Listener {
    private AsynchronousSocketChannel socketChannel;
    private String uuid;

    public ClientListener(Shell shell, Consumer<String> doInUI, byte[] address, int port) throws IOException {
        init(shell, doInUI, address, port);
    }

    private void init(Shell shell, Consumer<String> doInUI, byte[] address, int port) throws IOException {
        ClientMessageHandler handler = new ClientMessageHandler();
        socketChannel = AsynchronousSocketChannel.open();

        socketChannel.bind(new InetSocketAddress(port));

        InetAddress ia = InetAddress.getByAddress(address);

        uuid = UUID.randomUUID().toString();

        socketChannel.connect(new InetSocketAddress(ia, SERVER_PORT), uuid, new CompletionHandler<Void, String>() {

            @Override
            public void completed(Void result, String attachment) {
                final ByteBuffer bb = ByteBuffer.wrap((CONNECTING_GREET + attachment).getBytes(StandardCharsets.UTF_8));

                try {
                    socketChannel.write(bb).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                shell.getDisplay().syncExec(() -> {
                    doInUI.accept("Connected to Server, UUID: " + uuid);
                });

                socketChannel.read(bb, shell.getDisplay(), new CompletionHandler<Integer, Display>() {

                    @Override
                    public void completed(Integer result, Display display) {
                        if (result != -1) {
                            bb.flip();

                            StringBuffer sbuffer = new StringBuffer();

                            while (bb.hasRemaining()) {
                                sbuffer.append(StandardCharsets.UTF_8.decode(bb));
                            }
                            
                            display.syncExec(() -> doInUI.accept(handler.handleMessage(sbuffer.toString(), socketChannel)));

                            bb.clear();
                        }

                        if (socketChannel != null && socketChannel.isOpen()) {
                            socketChannel.read(bb, shell.getDisplay(), this);
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
                // TODO Auto-generated method stub

            }
        });
    }

    private class ClientMessageHandler implements Handler {
        @Override
        public String handleMessage(String message, AsynchronousSocketChannel channel) {
            if (message.startsWith(Protocol.MESSAGE_HEADER_LEFT_HALF)) {
                message = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "").replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, ": ");
            } else if (message.startsWith(Protocol.CONNECTING_GREET)) {
                return "";
            } else if (message.startsWith(Protocol.DISCONNECT)) {
                try {
                    disconnectWithoutNotification();
                } catch (IOException e) {
                    // Ignore.
                }
                
                return "Server disconnected the connection.";
            }
            
            return message;
        }
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen();
    }

    public void sendCommunicationData(String text) {
        final ByteBuffer bb = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        try {
            socketChannel.write(bb).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        sendCommunicationData(MESSAGE_HEADER_LEFT_HALF + uuid + MESSAGE_HEADER_RIGHT_HALF + message);
    }

    public void disconnect() throws IOException {
        if (isConnected()) {
            sendCommunicationData(DISCONNECT + uuid);
            socketChannel.close();
            socketChannel = null;
        }
    }
    
    public void disconnectWithoutNotification() throws IOException {
        if (isConnected()) {
            socketChannel.close();
            socketChannel = null;
        }
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}
