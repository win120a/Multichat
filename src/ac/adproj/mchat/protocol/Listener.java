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

package ac.adproj.mchat.protocol;

import ac.adproj.mchat.model.Protocol;

/**
 * Interface that represents a connection listener.
 * 
 * @author Andy Cheung
 * @date 2020/4/26
 * @see Protocol
 * @see AutoCloseable
 */
public interface Listener extends Protocol, AutoCloseable {
    /**
     * Send user message.
     * 
     * @param message The user message.
     * @param uuid    The user's UUID.
     */
    void sendMessage(String message, String uuid);

    /**
     * Send raw protocol message.
     * 
     * @param text The raw protocol message.
     * @param uuid    The user's UUID.
     */
    void sendCommunicationData(String text, String uuid);

    /**
     * Determine whether the server is connected by clients.
     * 
     * @return True if connected.
     */
    boolean isConnected();
}
