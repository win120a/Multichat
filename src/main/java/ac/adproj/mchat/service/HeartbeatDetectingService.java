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

package ac.adproj.mchat.service;

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.model.User;
import ac.adproj.mchat.protocol.ServerListener;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * Heartbeat detecting service.
 *
 * @author Andy Cheung
 */
@Slf4j
public class HeartbeatDetectingService implements Runnable {

    private static final long THIRTY_SECONDS = 30L * 1000L;

    private final ServerListener serverListener;

    public HeartbeatDetectingService(ServerListener serverListener) {
        this.serverListener = serverListener;
    }

    @Override
    public void run() {

        List<User> usersToRemove = new LinkedList<>();

        for (User u : UserManager.getInstance()) {
            if (System.currentTimeMillis() - u.getKeepAlivePackageTimestamp().get() > THIRTY_SECONDS) {
                // Broadcast message.

                serverListener.sendMessage("LOST connection to " + u.getName(),
                        Protocol.BROADCAST_MESSAGE_UUID);

                log.info("Lost connection from Name: {}, UUID: {}", u.getName(), u.getUuid());

                usersToRemove.add(u);
            }
        }

        for (var u : usersToRemove) {
            UserManager.getInstance().deleteUserProfile(u.getUuid());
        }
    }
}
