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

package ac.adproj.mchat.service;

import ac.adproj.mchat.model.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

/**
 * The Runnable task of user query service.
 *
 * @author Andy Cheung
 */
public class UserNameQueryService implements Runnable {
    private DatagramChannel dc;
    private boolean stopSelf;
    private final UserManager userManager;

    //// Buffers ////

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Protocol.BUFFER_SIZE);
    private final StringBuilder bufferOfMessage = new StringBuilder();

    public UserNameQueryService() throws IOException {
        dc = DatagramChannel.open();
        dc.configureBlocking(true);
        dc.bind(new InetSocketAddress(Protocol.SERVER_CHECK_DUPLICATE_PORT));
        userManager = UserManager.getInstance();
    }

    private void reInit() throws IOException {
        dc = DatagramChannel.open();
        dc.configureBlocking(true);
        dc.bind(new InetSocketAddress(Protocol.SERVER_CHECK_DUPLICATE_PORT));
    }

    @Override
    public void run() {

        while (!stopSelf || !Thread.interrupted()) {

            try {

                if (!dc.isOpen()) {
                    reInit();
                }

                SocketAddress address = dc.receive(byteBuffer);

                byteBuffer.flip();

                while (byteBuffer.hasRemaining()) {
                    bufferOfMessage.append(StandardCharsets.UTF_8.decode(byteBuffer));
                }

                String message = bufferOfMessage.toString();

                // Recycle the space.
                bufferOfMessage.delete(0, bufferOfMessage.length());
                byteBuffer.clear();

                if (!message.startsWith(Protocol.CHECK_DUPLICATE_REQUEST_HEADER)) {
                    continue;
                }

                handleQueryRequest(address, message);

            } catch (IOException e) {
                String name = e.getClass().getName();
                if (name.contains("ClosedByInterruptException") || name.contains("AsynchronousCloseException")) {
                    // ignore
                    return;
                }

                e.printStackTrace();
            }
        }

        try {
            dc.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private void handleQueryRequest(SocketAddress address, String message) throws IOException {
        String name = message.replace(Protocol.CHECK_DUPLICATE_REQUEST_HEADER, "");
        String result = userManager.containsName(name) ? Protocol.USER_NAME_DUPLICATED
                : Protocol.USER_NAME_NOT_EXIST;

        byteBuffer.put(result.getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();

        dc.send(byteBuffer, address);

        byteBuffer.clear();
    }

    /**
     * Stops the service.
     */
    public void stopSelf() {
        this.stopSelf = true;
    }
}