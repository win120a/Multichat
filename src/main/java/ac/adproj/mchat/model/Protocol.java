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

package ac.adproj.mchat.model;

/**
 * Constants that will be used in Listeners and Handlers.
 *
 * @author Andy Cheung
 */
public interface Protocol {
    /**
     * Port of (TCP) chatting service of Server.
     */
    int SERVER_PORT = 10240;

    /**
     * Port of UDP Users' Name Query Service.
     */
    int SERVER_CHECK_DUPLICATE_PORT = 10241;

    /**
     * Default port of client.
     */
    int CLIENT_DEFAULT_PORT = 10242;

    /**
     * Size of NIO buffers.
     */
    int BUFFER_SIZE = 1024;

    /**
     * <p>The left half of user registering message.</p>
     * <br />
     * <p>Message format: << CONNECT >>(UUID)>>>>(Name)<< CONNECT >></p>
     */
    String CONNECTING_GREET_LEFT_HALF = "<< CONNECT >>";

    /**
     * <p>The middle half of user registering message.</p>
     * <br />
     * <p>Message format: << CONNECT >>(UUID)>>>>(Name)<< CONNECT >></p>
     */
    String CONNECTING_GREET_MIDDLE_HALF = ">>>>>";

    /**
     * <p>The right half of user registering message.</p>
     * <br />
     * <p>Message format: << CONNECT >>(UUID)>>>>(Name)<< CONNECT >></p>
     */
    String CONNECTING_GREET_RIGHT_HALF = "<< CONNECT >>";

    /**
     * <p>The header of user logoff message.</p>
     * <br />
     * <p>Message format: << DISCONNECT >>(UUID)</p>
     */
    String DISCONNECT = "<< DISCONNECT >>";

    // << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)

    /**
     * <p>Left part of protocol string of incoming message.</p>
     * <br />
     * <p>Format: << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)</p>
     */
    String MESSAGE_HEADER_LEFT_HALF = "<< MESSAGE >>> <<<<";

    /**
     * <p>Middle part of protocol string of incoming message.</p>
     * <br />
     * <p>Format: << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)</p>
     */
    String MESSAGE_HEADER_MIDDLE_HALF = ">>>>>";

    /**
     * <p>Right part of protocol string of incoming message.</p>
     * <br />
     * <p>Format: << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)</p>
     */
    String MESSAGE_HEADER_RIGHT_HALF = " << MESSAGE >>";

    /**
     * The debug signal. Currently, when server listener receive the message, it prints the user list.
     */
    String DEBUG_MODE_STRING = "/// DEBUG ///";

    String BROADCAST_MESSAGE_UUID = "SERVER";

    // <<< DUP ? >>> (Name)
    String CHECK_DUPLICATE_REQUEST_HEADER = "<<< DUP ? >>> ";
    String USER_NAME_DUPLICATED = ">>> DUPLICATED <<< ";
    String USER_NAME_NOT_EXIST = "<<< Clear >>>";

    /**
     * Header of WebSocket user registering message.
     */
    String WEBSOCKET_UUID_HEADER = "<WS><<";

    /**
     * Tail of WebSocket user registering message.
     */
    String WEBSOCKET_UUID_TAIL = ">>";
}
