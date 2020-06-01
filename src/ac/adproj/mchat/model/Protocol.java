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
     * Size of buffers in NIO.
     */
    int BUFFER_SIZE = 1024;
    
    // << CONNECT >>(UUID)>>>>(Name)<< CONNECT >>
    String CONNECTING_GREET_LEFT_HALF = "<< CONNECT >>";
    String CONNECTING_GREET_MIDDLE_HALF = ">>>>>";
    String CONNECTING_GREET_RIGHT_HALF = "<< CONNECT >>";
    
    String DISCONNECT = "<< DISCONNECT >>";
    
    // << MESSAGE >>> <<<< (UUID) >>>> << MESSAGE >> (messageContent)
    String MESSAGE_HEADER_LEFT_HALF = "<< MESSAGE >>> <<<<";
    String MESSAGE_HEADER_MIDDLE_HALF = ">>>>>";
    String MESSAGE_HEADER_RIGHT_HALF = " << MESSAGE >>";
    
    String DEBUG_MODE_STRING = "/// DEBUG ///";
    
    String BROADCAST_MESSAGE_UUID = "SERVER";
    
    // <<< DUP ? >>> (Name)
    String CHECK_DUPLICATE_REQUEST_HEADER = "<<< DUP ? >>> ";
    String USER_NAME_DUPLICATED = ">>> DUPLICATED <<< ";
    String USER_NAME_NOT_EXIST = "<<< Clear >>>";
    
    String WEBSOCKET_UUID_HEADER = "<WS><<";
    String WEBSOCKET_UUID_TAIL = ">>";
}
