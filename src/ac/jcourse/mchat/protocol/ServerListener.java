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
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import ac.jcourse.mchat.model.User;
import ac.jcourse.mchat.protocol.handler.Handler;

/**
 * Listener class of Chatting Server.
 * @author Andy Cheung
 */
public class ServerListener implements Listener {

    private AsynchronousServerSocketChannel serverSocketChannel = null;
    
    private ExecutorService threadPool;
    private Map<String, User> userProfile = new ConcurrentHashMap<>(16);
    
    private Shell shell;
    private Consumer<String> doInUI;
    
    private int threadNumber = 0;

    public ServerListener(Shell shell, Consumer<String> doInUI) throws IOException {
        this.shell = shell;
        this.doInUI = doInUI;
        
        init(shell, doInUI);
    }
    
    private void readMessage(ByteBuffer bb, Handler handler, Integer result, AsynchronousSocketChannel channel) {

        if (result != -1) {

            bb.flip();

            StringBuffer sbuffer = new StringBuffer();

            while (bb.hasRemaining()) {
                sbuffer.append(StandardCharsets.UTF_8.decode(bb));
            }

            String message = handler.handleMessage(sbuffer.toString(), channel);

            shell.getDisplay().syncExec(() -> {
                doInUI.accept(message);
            });
            
            
            bb.clear();
        }
    }

    private void init(Shell shell, Consumer<String> doInUI) throws IOException {
        ServerMessageHandler handler = new ServerMessageHandler();

        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>(16);
        
        ThreadFactory threadFactory = r -> {
            threadNumber++;
            return new Thread(r, "PoolThread - SrvListener - " + threadNumber);
        };

        threadPool = new ThreadPoolExecutor(4, 16, 3000, TimeUnit.MILLISECONDS, bq, threadFactory);

        AsynchronousChannelGroup acg = AsynchronousChannelGroup.withThreadPool(threadPool);
        
        serverSocketChannel = AsynchronousServerSocketChannel.open(acg);

        serverSocketChannel.bind(new InetSocketAddress(Protocol.SERVER_PORT));

        serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                final ByteBuffer bb = ByteBuffer.allocate(1024);

                /* Handle messages. */
                result.read(bb, result, new CompletionHandler<Integer, AsynchronousSocketChannel>() {

                    @Override
                    public void completed(Integer result, AsynchronousSocketChannel channel) {
                        readMessage(bb, handler, result, channel);
                        
                        if (channel.isOpen()) {
                            channel.read(bb, channel, this);
                        }
                    }

                    @Override
                    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                        exc.printStackTrace();
                        userProfile.entrySet().removeIf((user) -> user.getValue().getChannel().equals(channel));
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
        });
    }

    /**
     * Message Handler in Server.
     * @author Andy Cheung
     * @date 2020/4/26
     */
    private class ServerMessageHandler implements Handler {
        @Override
        public String handleMessage(String message, AsynchronousSocketChannel channel) {
            // Greet message.
            if (message.startsWith(Protocol.CONNECTING_GREET_LEFT_HALF)) {
                String[] data = message.replace(Protocol.CONNECTING_GREET_LEFT_HALF, "")
                        .replace(Protocol.CONNECTING_GREET_RIGHT_HALF, "").split(Protocol.CONNECTING_GREET_MIDDLE_HALF);

                String uuid = data[0];
                String name = data[1];

                User userObject = new User(uuid, channel, name);

                userProfile.put(uuid, userObject);

                return "Client: " + uuid + " (" + name + ") Connected.";
            } else if (message.startsWith(Protocol.DEBUG_MODE_STRING)) {
                System.out.println(userProfile);
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
            } else if (message.startsWith(Protocol.MESSAGE_HEADER_LEFT_HALF)) {
                
                String[] data = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "").split(Protocol.MESSAGE_HEADER_MIDDLE_HALF);
                
                if (data.length < 2) {
                    return "";
                }
                
                String uuid = data[0];
                String m = data[1];
                
                int messageLength = message.getBytes(StandardCharsets.UTF_8).length;
                String nameOnlyMessage = message.replace(uuid, userProfile.get(uuid).getName());
                
                ByteBuffer bb = ByteBuffer.wrap(nameOnlyMessage.getBytes(StandardCharsets.UTF_8));
                
                for (User u : userProfile.values()) {
                    try {
                        if (!uuid.equals(u.getUuid())) {
                            bb.rewind();
                            
                            int writtenBytes = u.getChannel().write(bb).get();
                            
                            System.out.println("Message Length: " + messageLength + "  Result: " + writtenBytes);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
                message = userProfile.get(uuid).getName() + ": " + m;
            }

            return message;
        }
    }

    @Override
    public boolean isConnected() {
        if (userProfile.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void sendCommunicationData(String text, String uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        
        if (uuid.equals(Protocol.BROADCAST_MESSAGE_UUID)) {
            for (User u : userProfile.values()) {
                try {
                    bb.rewind();
                    
                    while (bb.hasRemaining()) {
                        u.getChannel().write(bb).get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            try {
                userProfile.get(uuid).getChannel().write(bb).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendMessage(String message, String uuid) {
        sendCommunicationData(MESSAGE_HEADER_LEFT_HALF + Protocol.BROADCAST_MESSAGE_UUID +
                                  MESSAGE_HEADER_MIDDLE_HALF + MESSAGE_HEADER_RIGHT_HALF + message, uuid);
    }

    public void disconnect(String uuid) throws IOException {
        userProfile.get(uuid).getChannel().close();
        userProfile.remove(uuid);
    }

    public void disconnectAll() throws IOException {
        userProfile.forEach((K, V) -> {
            try {
                final ByteBuffer bb = ByteBuffer
                        .wrap((Protocol.DISCONNECT + "SERVER").getBytes(StandardCharsets.UTF_8));

                V.getChannel().write(bb).get();
                V.getChannel().close();
            } catch (IOException | InterruptedException | ExecutionException e) {
                // Ignore
            }
        });
        
        userProfile.clear();
    }

    @Override
    public void close() throws Exception {
        threadPool.shutdownNow();
        userProfile.clear();
        serverSocketChannel.close();
    }
}
