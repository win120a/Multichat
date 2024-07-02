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

package ac.adproj.mchat.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data class that wraps the user information.
 *
 * @author Andy Cheung
 * @date 2020-4-27
 */
@Data
public final class User {
    private String uuid;

    private AsynchronousSocketChannel channel;

    private String name;

    @Setter(value = AccessLevel.PRIVATE)
    private AtomicLong keepAlivePackageTimestamp;

    public User(String uuid, AsynchronousSocketChannel channel, String name) {
        super();
        this.uuid = uuid;
        this.channel = channel;
        this.name = name;
        this.keepAlivePackageTimestamp = new AtomicLong(System.currentTimeMillis());
    }
}
