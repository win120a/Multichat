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

import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

import static ac.adproj.mchat.handler.MessageType.getMessageType;

/**
 * Message Handler of client.
 * 
 * @author Andy Cheung
 */
public class ClientMessageHandler implements Handler {
    private final Consumer<Void> forceLogoffCallback;

    /**
     * Get instance.
     * 
     * @param forceLogoffCallback Callback method wrapper when server urges the client to
     *                            be disconnected.
     */
    public ClientMessageHandler(Consumer<Void> forceLogoffCallback) {
        super();
        this.forceLogoffCallback = forceLogoffCallback;
    }

    @Override
    public String handleMessage(String message, AsynchronousSocketChannel channel) {

        switch (getMessageType(message)) {
            case INCOMING_MESSAGE:
                message = message.replace(Protocol.MESSAGE_HEADER_LEFT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_RIGHT_HALF, "")
                        .replace(Protocol.MESSAGE_HEADER_MIDDLE_HALF, ": ");
                break;

            case LOGOFF:
                forceLogoffCallback.accept(null);
                message = "Server closed the connection.";
                break;

            case UNKNOWN:
            default:
                message = "";
                break;
        }

        return message;
    }
}