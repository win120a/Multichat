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

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.model.User;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.UserManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * Message Handler of Server.
 *
 * @author Andy Cheung
 * @since 2020/5/24
 */
@Slf4j
public class ServerMessageHandler implements Handler {
    private UserManager userManager = UserManager.getInstance();
    private ServerListener listener;

    private static final Pattern PATTERN_OF_PRIVATE_CHATTING_MESSAGE = Pattern.compile("[@].*[#]");

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
                        .replace(Protocol.CONNECTING_GREET_RIGHT_HALF, "")
                        .split(Protocol.CONNECTING_GREET_MIDDLE_HALF);

                String uuid = data[0];
                String name = data[1];

                User userObject = new User(uuid, channel, name);

                userManager.register(userObject);

                return "Client: " + uuid + " (" + name + ") Connected.";

            case DEBUG:
                System.out.println(userManager.toString());
                return "";

            case LOGOFF:
                SoftReference<String> uuidToLogoff = new SoftReference<>(message.replace(Protocol.DISCONNECT, ""));

                try {
                    log.info("Disconnecting: " + uuidToLogoff.get());
                    listener.disconnect(uuidToLogoff.get());
                } catch (IOException e) {
                    log.error("Error when handling the logoff of uid: " + uuidToLogoff.get(), e);
                }

                return "Client: " + message.replace(Protocol.DISCONNECT, "") + " Disconnected.";

            case INCOMING_MESSAGE:

                String[] messageData = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "")
                        .split(Protocol.MESSAGE_HEADER_MIDDLE_HALF);

                if (messageData.length < 2) {
                    return "";
                }

                String fromUuid = messageData[0];
                String messageText = messageData[1];
                String nameOnlyMessage = message.replace(fromUuid, userManager.getName(fromUuid));

                var matcherOfMessageText = PATTERN_OF_PRIVATE_CHATTING_MESSAGE.matcher(messageText);

                // Private chatting message.
                if (matcherOfMessageText.find()) {
                    var target = matcherOfMessageText.group()
                            .replace("@", "")
                            .replace("#", "");

                    var targetUser = UserManager.getInstance().findUuidByName(target);

                    if (targetUser.isPresent()) {
                        listener.sendMessage(nameOnlyMessage, targetUser.get());
                    } else {
                        listener.sendMessage("INVALID username", fromUuid);
                    }
                } else {
                    ByteBuffer bb = ByteBuffer.wrap(nameOnlyMessage.getBytes(StandardCharsets.UTF_8));

                    for (User u : userManager.userProfileValueSet()) {

                        Future<Integer> futureOfWriting;

                        try {
                            if (!fromUuid.equals(u.getUuid())) {
                                bb.rewind();
                                futureOfWriting = u.getChannel().write(bb);
                                futureOfWriting.get();
                            }
                        } catch (InterruptedException interruptedException) {

                            // Propagate the interruption status.
                            Thread.currentThread().interrupt();

                            log.error("Got interrupted when waiting for message sent.", interruptedException);

                        } catch (ExecutionException executionException) {

                            log.error("Exception occurred when waiting for message sent.", executionException);
                        }
                    }
                }

                message = userManager.getName(fromUuid) + ": " + messageText;
                break;

            case KEEP_ALIVE:
                var keepAliveMessageUuid = msgTyp.tokenize(message).get("uuid");

                if (userManager.containsUuid(keepAliveMessageUuid)) {
                    log.info("Got KA message from UUID: {}", keepAliveMessageUuid);

                    userManager.lookup(keepAliveMessageUuid)
                            .getKeepAlivePackageTimestamp()
                            .getAndUpdate(prev -> {
                                long millis = System.currentTimeMillis();

                                return Math.max(millis, prev);
                            });

                    System.out.println(userManager.lookup(keepAliveMessageUuid));
                }

                return "";

            case UNKNOWN:
            default:
                return message;
        }

        return message;
    }
}