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
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ac.adproj.mchat.handler.ClientMessageHandler;
import ac.adproj.mchat.model.Protocol;

/**
 * Listener class of Client.
 * 
 * @author Andy Cheung
 */
public class ClientListener implements Listener {
    private AsynchronousSocketChannel socketChannel;
    private String uuid;
    private String name;

    public ClientListener(Shell shell, Consumer<String> doInUI, byte[] address, int port, String username)
            throws IOException {
        this.name = username;
        init(shell, doInUI, address, port, username);
    }

    private void init(Shell shell, Consumer<String> doInUI, byte[] address, int port, String username)
            throws IOException {
        socketChannel = AsynchronousSocketChannel.open();

        socketChannel.bind(new InetSocketAddress(port));

        InetAddress ia = InetAddress.getByAddress(address);

        uuid = UUID.randomUUID().toString();

        initNIOSocketConnection(shell, doInUI, ia, username);
    }

    public static boolean checkNameDuplicates(byte[] serverAddress, String name) throws IOException {
        DatagramChannel dc = DatagramChannel.open();
        
        ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
        
        bb.put((Protocol.CHECK_DUPLICATE_REQUEST_HEADER + name).getBytes(StandardCharsets.UTF_8));
        bb.flip();

        StringBuffer buffer = new StringBuffer();
        
        try {

            dc.configureBlocking(true);
            dc.send(bb, new InetSocketAddress(InetAddress.getByAddress(serverAddress), Protocol.SERVER_CHECK_DUPLICATE_PORT));
            
            bb.clear();
            
            dc.receive(bb);
            
            bb.flip();
            
            while (bb.hasRemaining()) {
                buffer.append(StandardCharsets.UTF_8.decode(bb));
            }
            
            if (buffer.toString().startsWith(Protocol.USER_NAME_NOT_EXIST)) {
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            dc.close();
        }
    }

    private void initNIOSocketConnection(Shell shell, Consumer<String> doInUI, InetAddress ia, String username) {
        ClientMessageHandler handler = new ClientMessageHandler((v) -> {
            try {
                disconnectWithoutNotification();
            } catch (IOException e) {
                e.printStackTrace();
                shell.getDisplay().syncExec(() -> MessageDialog.openError(shell, "出错", "下线出错：" + e.getMessage()));
            }
        });

        socketChannel.connect(new InetSocketAddress(ia, SERVER_PORT), uuid, new CompletionHandler<Void, String>() {

            @Override
            public void completed(Void result, String attachment) {
                String greetMessage = CONNECTING_GREET_LEFT_HALF + attachment + CONNECTING_GREET_MIDDLE_HALF + username;
                final ByteBuffer greetBuffer = ByteBuffer.wrap(greetMessage.getBytes());

                try {
                    socketChannel.write(greetBuffer).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                shell.getDisplay().syncExec(() -> {
                    doInUI.accept("Connected to Server, UserName: " + username + ", UUID: " + uuid);
                });

                final ByteBuffer buffer = ByteBuffer.allocate(Protocol.BUFFER_SIZE);

                socketChannel.read(buffer, shell.getDisplay(), new CompletionHandler<Integer, Display>() {

                    @Override
                    public void completed(Integer result, Display display) {
                        if (result != -1) {
                            buffer.flip();

                            StringBuffer sbuffer = new StringBuffer();

                            while (buffer.hasRemaining()) {
                                sbuffer.append(StandardCharsets.UTF_8.decode(buffer));
                            }

                            display.syncExec(
                                    () -> doInUI.accept(handler.handleMessage(sbuffer.toString(), socketChannel)));

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