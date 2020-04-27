package ac.jcourse.mchat.protocol;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ac.jcourse.mchat.protocol.handler.Handler;

public class ServerListener implements Listener {

    private AsynchronousServerSocketChannel serverSocketChannel = null;
    private AsynchronousSocketChannel socketChannel = null;
    private ExecutorService threadPool;
    private Map<String, AsynchronousSocketChannel> channels = new HashMap<>();

    public ServerListener(Shell shell, Consumer<String> doInUI) throws IOException {
        init(shell, doInUI);
    }
    
    private synchronized void setSocketChannel(AsynchronousSocketChannel channel) {
        this.socketChannel = channel;
    }
    
    private synchronized AsynchronousSocketChannel getSocketChannel() {
        return socketChannel;
    }

    private void init(Shell shell, Consumer<String> doInUI) throws IOException {
        ServerMessageHandler handler = new ServerMessageHandler();

        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>(16);

        threadPool = new ThreadPoolExecutor(4, 16, 3000, TimeUnit.MILLISECONDS, bq);

        AsynchronousChannelGroup acg = AsynchronousChannelGroup.withThreadPool(threadPool);

        serverSocketChannel = AsynchronousServerSocketChannel.open(acg);

        serverSocketChannel.bind(new InetSocketAddress(Protocol.SERVER_PORT));

        new Thread(() -> serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                setSocketChannel(result);

                final ByteBuffer bb = ByteBuffer.allocate(1024);

                socketChannel.read(bb, shell.getDisplay(), new CompletionHandler<Integer, Display>() {

                    @Override
                    public void completed(Integer result, Display display) {

                        if (result != -1) {

                            bb.flip();

                            StringBuffer sbuffer = new StringBuffer();

                            while (bb.hasRemaining()) {
                                sbuffer.append(StandardCharsets.UTF_8.decode(bb));
                            }

                            String message = handler.handleMessage(sbuffer.toString(), socketChannel);

                            display.syncExec(() -> {
                                doInUI.accept(message);
                            });

                            bb.clear();
                        }
                        
                        if (socketChannel.isOpen()) {
                            socketChannel.read(bb, display, this);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Display attachment) {
                        channels.entrySet().removeIf((val) -> val.getValue().equals(socketChannel));
                    }
                });
                
                serverSocketChannel.accept(this, this);
            }

            @Override
            public void failed(Throwable exc, Object o) {
                exc.printStackTrace();

                shell.getDisplay().syncExec(() -> {
                    MessageDialog.openError(shell, "出错", "监听出错：" + exc.getMessage());
                });
            }
        }), "Srv-ListeningThread").start();
    }

    private class ServerMessageHandler implements Handler {
        @Override
        public String handleMessage(String message, AsynchronousSocketChannel channel) {
            if (message.startsWith(Protocol.CONNECTING_GREET)) {
                channels.put(message.replace(Protocol.CONNECTING_GREET, ""), channel);
                return "Client: " + message.replace(Protocol.CONNECTING_GREET, "") + " Connected.";
            } else if (message.startsWith(Protocol.DEBUG_MODE_STRING)) {
                System.out.println(channels);
                return "";
                
            } else if (message.startsWith(Protocol.DISCONNECT)) {
                SoftReference<String> uuid = new SoftReference<String>(message.replace(Protocol.DISCONNECT, ""));
                
                try {
                    System.out.println("Disconnecting: " + uuid.get());
                    disconnect(uuid.get());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                return "Client: " + message.replace(Protocol.DISCONNECT, "") + " Disconnected.";
            } else if (message.startsWith(Protocol.MESSAGE_HEADER_LEFT_HALF)){
                message = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "").replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, ": ");
            }
            
            return message;
        }
    }

    public boolean isConnected() {
        if (socketChannel == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void sendCommunicationData(String text) {
        final ByteBuffer bb = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        try {
            socketChannel.write(bb).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void sendMessage(String message) {
        sendCommunicationData(MESSAGE_HEADER_LEFT_HALF + "SERVER" + MESSAGE_HEADER_RIGHT_HALF + message);
    }
    
    public void disconnect(String uuid) throws IOException {
        channels.get(uuid).close();
        channels.remove(uuid);
    }
    
    public void disconnectAll() throws IOException {
        channels.forEach((K, V) -> {
            try {
                final ByteBuffer bb = ByteBuffer.wrap((Protocol.DISCONNECT + "SERVER").getBytes(StandardCharsets.UTF_8));

                V.write(bb).get();
                V.close();
            } catch (IOException | InterruptedException | ExecutionException e) {
                // Ignore
            }
        });
        channels.clear();
    }

    @Override
    public void close() throws Exception {
        threadPool.shutdownNow();
        channels.clear();
        serverSocketChannel.close();
    }
}
