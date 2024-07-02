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

package ac.adproj.mchat.handler;

import ac.adproj.mchat.model.Protocol;

import java.util.Collections;
import java.util.Map;

/**
 * The message tokenizer & message type enumeration.
 *
 * @author Andy Cheung
 * @since 2020/5/19
 */
public enum MessageType {
    /**
     * User registration (connecting greet).
     */
    REGISTER(Protocol.CONNECTING_GREET_LEFT_HALF) {
        @Override
        public Map<String, String> tokenize(String message) {
            String[] data = message.replace(Protocol.CONNECTING_GREET_LEFT_HALF, "")
                    .replace(Protocol.CONNECTING_GREET_RIGHT_HALF, "").split(Protocol.CONNECTING_GREET_MIDDLE_HALF);

            String uuid = data[0];
            String name = data[1];

            return Map.of("uuid", uuid, "name", name);
        }
    },

    /**
     * User logoff (disconnect).
     */
    LOGOFF(Protocol.DISCONNECT) {
        @Override
        public Map<String, String> tokenize(String message) {
            String targetUuid = message.replace(Protocol.DISCONNECT, "");

            return Map.of("uuid", targetUuid);
        }
    },

    /**
     * Incoming User message.
     */
    INCOMING_MESSAGE(Protocol.MESSAGE_HEADER_LEFT_HALF) {
        @Override
        public Map<String, String> tokenize(String message) {
            String[] msgData = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                    .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "").split(Protocol.MESSAGE_HEADER_MIDDLE_HALF);

            final int validDataArrayLength = 2;

            if (msgData.length < validDataArrayLength) {
                return Collections.emptyMap();
            }

            String fromUuid = msgData[0];
            String messageText = msgData[1];

            return Map.of("uuid", fromUuid, "messageText", messageText);
        }
    },

    /**
     * Debug mode. (Trigger server to print out some debugging information.)
     */
    DEBUG(Protocol.DEBUG_MODE_STRING) {
        @Override
        public Map<String, String> tokenize(String message) {
            return Map.of();
        }
    },

    /**
     * User name querying request.
     */
    USERNAME_QUERY_REQUEST(Protocol.CHECK_DUPLICATE_REQUEST_HEADER) {
        @Override
        public Map<String, String> tokenize(String message) {
            return Map.of("username", message.replace(Protocol.CHECK_DUPLICATE_REQUEST_HEADER, ""));
        }
    },

    KEEP_ALIVE(Protocol.KEEP_ALIVE_HEADER) {
        @Override
        public Map<String, String> tokenize(String message) {
            return Map.of("uuid", message.replace(Protocol.KEEP_ALIVE_HEADER, "")
                    .replace(Protocol.KEEP_ALIVE_TAIL, ""));
        }
    },

    /**
     * Unknown message.
     */
    UNKNOWN("\\\\") {
        @Override
        public Map<String, String> tokenize(String message) {
            return Map.of();
        }
    };

    private final String headerString;

    MessageType(String headerString) {
        this.headerString = headerString;
    }

    /**
     * Obtain the corresponding message type object.
     *
     * @param message The raw protocol message.
     * @return The corresponding MessageType object.
     */
    public static MessageType getMessageType(String message) {

        for (var type : MessageType.values()) {
            if (message.startsWith(type.headerString)) {
                return type;
            }
        }

        return MessageType.UNKNOWN;
    }

    /**
     * Message tokenizer entrance method.
     *
     * @param message Raw protocol method.
     * @return Tokenized elements.
     */
    public abstract Map<String, String> tokenize(String message);
}
