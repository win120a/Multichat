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

package ac.adproj.mchat.handler;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.model.User;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.UserManager;

/**
 * Message Handler of Server.
 * 
 * @author Andy Cheung
 * @since 2020/5/24
 */
public class ServerMessageHandler implements Handler {
    private UserManager userManager = UserManager.getInstance();
    private ServerListener listener;

    public ServerMessageHandler(ServerListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public String handleMessage(String message, AsynchronousSocketChannel channel) {
        MessageType msgTyp = MessageType.getMessageType(message);

        switch (msgTyp) {
            case REGISTER:
                String[] data = message.replace(Protocol.CONNECTING_GREET_LEFT_HALF, "")
                        .replace(Protocol.CONNECTING_GREET_RIGHT_HALF, "").split(Protocol.CONNECTING_GREET_MIDDLE_HALF);

                String uuid = data[0];
                String name = data[1];

                User userObject = new User(uuid, channel, name);

                userManager.register(userObject);

                return "Client: " + uuid + " (" + name + ") Connected.";

            case DEBUG:
                System.out.println(userManager.toString());
                return "";

            case LOGOFF:
                SoftReference<String> uuid_logoff = new SoftReference<String>(message.replace(Protocol.DISCONNECT, ""));

                try {
                    System.out.println("Disconnecting: " + uuid_logoff.get());
                    listener.disconnect(uuid_logoff.get());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return "Client: " + message.replace(Protocol.DISCONNECT, "") + " Disconnected.";

            case INCOMING_MESSAGE:

                String[] messageData = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "").split(Protocol.MESSAGE_HEADER_MIDDLE_HALF);

                if (messageData.length < 2) {
                    return "";
                }

                String fromUuid = messageData[0];
                String messageText = messageData[1];

                String nameOnlyMessage = message.replace(fromUuid, userManager.getName(fromUuid));

                ByteBuffer bb = ByteBuffer.wrap(nameOnlyMessage.getBytes(StandardCharsets.UTF_8));

                for (User u : userManager.userProfileValueSet()) {
                    try {
                        if (!fromUuid.equals(u.getUuid())) {
                            bb.rewind();
                            u.getChannel().write(bb).get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                message = userManager.getName(fromUuid) + ": " + messageText;
                break;

            case UNKNOWN:
            default:
                return message;
        }

        return message;
    }
}