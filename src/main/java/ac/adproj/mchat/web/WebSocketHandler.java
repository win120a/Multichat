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

package ac.adproj.mchat.web;

import ac.adproj.mchat.handler.MessageType;
import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.MessageDistributor;
import ac.adproj.mchat.service.MessageDistributor.SubscriberCallback;
import ac.adproj.mchat.service.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static ac.adproj.mchat.handler.MessageType.REGISTER;
import static ac.adproj.mchat.handler.MessageType.USERNAME_QUERY_REQUEST;
import static ac.adproj.mchat.model.Protocol.*;

/**
 * Message Handler of WebSocket.
 *
 * @author Andy Cheung
 * @since 2020/5/18
 */
@Slf4j
public class WebSocketHandler implements WebSocketListener {
    private static final Pattern PATTERN_OF_PRIVATE_CHATTING_MESSAGE = Pattern.compile("[@].*[#]");
    private static final Set<WebSocketHandler> connections;
    // UUID, Name
    private static final Map<String, String> nameBindings;

    static {
        connections = Collections.synchronizedSet(new HashSet<>(16));
        nameBindings = new ConcurrentHashMap<>(16);
        MessageDistributor.getInstance().registerSubscriber(new WebSocketBridge());
    }

    private final String uuid = UUID.randomUUID().toString();
    private Session session;
    private String nickname;

    public static boolean isConnected() {
        return !connections.isEmpty();
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        /* only interested in text messages */
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        connections.remove(this);
        nameBindings.remove(uuid);
        UserManager.getInstance().undoReserveName(nickname);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        cause.printStackTrace(System.err);
    }

    @Override
    public void onWebSocketText(String message) {
        var um = UserManager.getInstance();

        switch (MessageType.getMessageType(message)) {

            case INCOMING_MESSAGE -> handleIncomingMessage(message, um);

            case REGISTER -> handleRegister(message);

            case USERNAME_QUERY_REQUEST -> handleUsernameQueryRequest(message);

            default -> {
                // Don't do anything.
            }
        }
    }

    private void handleUsernameQueryRequest(String message) {
        String name = USERNAME_QUERY_REQUEST.tokenize(message).get("username");

        try {
            session.getRemote()
                    .sendString(UserManager.getInstance().containsName(name) ? Protocol.USER_NAME_DUPLICATED
                            : Protocol.USER_NAME_NOT_EXIST);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleRegister(String message) {
        Map<String, String> result = REGISTER.tokenize(message);

        if (UserManager.getInstance().reserveName(result.get("name"))) {
            nameBindings.put(result.get("uuid"), result.get("name"));
        }

        nickname = result.get("name");
    }

    private void handleIncomingMessage(String message, UserManager um) {
        String protoMsg = message.replace(uuid, nickname);

        var matcherOfMessageText = PATTERN_OF_PRIVATE_CHATTING_MESSAGE.matcher(message);

        // Private chatting message.
        if (matcherOfMessageText.find()) {
            var targetName = matcherOfMessageText.group()
                    .replace("@", "")
                    .replace("#", "");

            connections.stream()
                    .filter(x -> x.nickname.equals(targetName))
                    .findFirst()
                    .ifPresentOrElse(x -> {
                        try {
                            x.session.getRemote().sendString(protoMsg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, () -> {
                        try {
                            var tokenizedMessage = MessageType.INCOMING_MESSAGE.tokenize(message);

                            var messageText = tokenizedMessage.get("messageText").split("#", 2)[1];

                            ServerListener.getInstance().sendCommunicationData(MESSAGE_HEADER_LEFT_HALF +
                                    this.nickname + " -> " + targetName + " (私聊)" +
                                    MESSAGE_HEADER_MIDDLE_HALF + MESSAGE_HEADER_RIGHT_HALF +
                                    messageText, um.findUuidByName(targetName).get());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            return;
        }

        try {
            MessageDistributor.getInstance().sendRawProtocolMessage(protoMsg);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        broadcastMessage(protoMsg);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        connections.add(this);

        try {
            session.getRemote().sendString(Protocol.WEBSOCKET_UUID_HEADER + uuid + Protocol.WEBSOCKET_UUID_TAIL);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        if ((session != null) && (session.isOpen())) {
            for (WebSocketHandler h : connections) {
                if (h != this) {
                    try {
                        h.session.getRemote().sendString(message);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            ServerListener.getInstance().sendCommunicationData(message, Protocol.BROADCAST_MESSAGE_UUID);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        WebSocketHandler other = (WebSocketHandler) obj;
        return Objects.equals(uuid, other.uuid);
    }

    public static class WebSocketBridge implements SubscriberCallback {
        @Override
        public void onMessageReceived(String uiMessage) {

            if (uiMessage.isEmpty()) {
                return;
            }

            String name = uiMessage.split("\\s*:\\s*")[0];

            for (WebSocketHandler conn : connections) {
                if (conn.nickname.equals(name) || conn.uuid.equals(name)) {
                    continue;
                }

                try {
                    conn.session.getRemote().sendString(uiMessage);
                } catch (IOException e) {
                    log.error("Failed in sending websocket message in broadcasting. [UUID = " + conn.uuid + "]", e);
                }
            }
        }
    }
}
