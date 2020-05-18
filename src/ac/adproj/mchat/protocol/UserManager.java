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

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ac.adproj.mchat.model.User;

public class UserManager implements Iterable<User> {
    private static UserManager instance;
    
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }

        return instance;
    }

    private Map<String, User> userProfile;

    private Set<String> names;

    private UserManager() {
        userProfile = new ConcurrentHashMap<>(16);
        names = Collections.synchronizedSet(new HashSet<>());
    }

    public void clearAllProfiles() {
        userProfile.clear();
        names.clear();
    }

    public boolean containsName(String name) {
        return names.contains(name);
    }

    public User deleteUserProfile(String uuid) {
        names.remove(lookup(uuid).getName());
        return userProfile.remove(uuid);
    }

    public String getName(String uuid) {
        return lookup(uuid).getName();
    }

    public boolean isEmptyUserProfile() {
        return userProfile.isEmpty();
    }

    @Override
    public Iterator<User> iterator() {
        return userProfile.values().iterator();
    }

    public User lookup(String uuid) {
        return userProfile.get(uuid);
    }

    public void register(String uuid, String name, AsynchronousSocketChannel channel) {
        register(new User(uuid, channel, name));
    }

    public void register(User u) {
        userProfile.put(u.getUuid(), u);
        names.add(u.getName());
    }

    @Override
    public String toString() {
        return userProfile.toString();
    }

    public Collection<User> userProfileValueSet() {
        return userProfile.values();
    }
}
