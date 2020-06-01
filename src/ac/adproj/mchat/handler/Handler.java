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

import java.nio.channels.AsynchronousSocketChannel;

/**
 * Message handler interface.
 * 
 * @author Andy Cheung
 * @since 2020.4.26
 */
public interface Handler {
    /**
     * Handle protocol messages.
     * 
     * @param message Raw protocol message.
     * @param channel The socket channel of the other side.
     * @return The text will be transmitted to UI.
     */
    default String handleMessage(String message, AsynchronousSocketChannel channel) {
        return message;
    }
}
