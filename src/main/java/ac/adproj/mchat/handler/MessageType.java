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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ac.adproj.mchat.model.Protocol;

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
    REGISTER,

    /**
     * User logoff (disconnect).
     */
    LOGOFF,

    /**
     * Incoming User message.
     */
    INCOMING_MESSAGE,

    /**
     * Debug mode. (Trigger server to print out some debugging information.)
     */
    DEBUG,

    /**
     * User name querying request.
     */
    USERNAME_QUERY_REQUEST,

    /**
     * Unknown message.
     */
    UNKNOWN;

    /**
     * Obtain the corresponding message type object.
     * 
     * @param message The raw protocol message.
     * @return The corresponding MessageType object.
     */
    public static MessageType getMessageType(String message) {
        if (message.startsWith(Protocol.CONNECTING_GREET_LEFT_HALF)) {
            return REGISTER;
        } else if (message.startsWith(Protocol.DEBUG_MODE_STRING)) {
            return DEBUG;
        } else if (message.startsWith(Protocol.DISCONNECT)) {
            return LOGOFF;
        } else if (message.startsWith(Protocol.MESSAGE_HEADER_LEFT_HALF)) {
            return INCOMING_MESSAGE;
        } else if (message.startsWith(Protocol.CHECK_DUPLICATE_REQUEST_HEADER)) {
            return USERNAME_QUERY_REQUEST;
        }
        return MessageType.UNKNOWN;
    }

    /**
     * Message tokenizer entrance method.
     * 
     * @param message Raw protocol method.
     * @return Tokenized elements.
     */
    public Map<String, String> tokenize(String message) {
        Map<String, String> ret = null;

        switch (this) {
            case REGISTER:
                String[] data = message.replace(Protocol.CONNECTING_GREET_LEFT_HALF, "")
                        .replace(Protocol.CONNECTING_GREET_RIGHT_HALF, "").split(Protocol.CONNECTING_GREET_MIDDLE_HALF);

                String uuid = data[0];
                String name = data[1];

                ret = of("uuid", uuid, "name", name);

                break;

            case LOGOFF:
                String targetUuid = message.replace(Protocol.DISCONNECT, "");

                ret = of("uuid", targetUuid);

                break;

            case INCOMING_MESSAGE:
                String[] msgData = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "").split(Protocol.MESSAGE_HEADER_MIDDLE_HALF);

                final int vaildDataArrayLength = 2;

                if (msgData.length < vaildDataArrayLength) {
                    return Collections.emptyMap();
                }

                String fromUuid = msgData[0];
                String messageText = msgData[1];

                ret = of("uuid", fromUuid, "messageText", messageText);

                break;

            case USERNAME_QUERY_REQUEST:
                ret = of("username", message.replace(Protocol.CHECK_DUPLICATE_REQUEST_HEADER, ""));
                break;

            case UNKNOWN:
            default:
                ret = Collections.emptyMap();
                break;
        }

        return ret;
    }

    /**
     * Return a read-only Map with one entry. (Like Map.of)
     * 
     * @param <K> Type of key.
     * @param <V> Type of value.
     * @param k1  The first key.
     * @param v1  The first value.
     * @return A read-only Map that contains only one entry.
     */
    private <K, V> Map<K, V> of(K k1, V v1) {
        HashMap<K, V> hm = new HashMap<>(1);

        hm.put(k1, v1);

        return Collections.unmodifiableMap(hm);
    }

    /**
     * Return a read-only Map with two entries. (Like Map.of)
     * 
     * @param <K> Type of key.
     * @param <V> Type of value.
     * @param k1  The first key.
     * @param v1  The first value.
     * @param k2  The second key.
     * @param v2  The second value.
     * @return A read-only Map that contains two specified entries.
     */
    private <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        HashMap<K, V> hm = new HashMap<>(2);

        hm.put(k1, v1);
        hm.put(k2, v2);

        return Collections.unmodifiableMap(hm);
    }
}
