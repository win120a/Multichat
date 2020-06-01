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

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ac.adproj.mchat.model.User;

/**
 * <p>
 * User Manager.
 * </p>
 * 
 * <p>
 * Intended to share user register information between TCP server & WebSocket
 * Server.
 * </p>
 * 
 * <p>
 * <b>Implementation Note: Although the user names is shared between TCP and
 * WebSocket server,</b> <b>the user information of WebSocket Server isn't
 * managed by this class.</b><br />
 * </p>
 * 
 * @author Andy Cheung
 * @since 2020.5.24
 */
public class UserManager implements Iterable<User> {
    private static UserManager instance;

    /**
     * Obtain the only instance.
     * 
     * @return The instance.
     */
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }

        return instance;
    }

    private Map<String, User> userProfile;

    private Set<String> names;

    // Store the reserved names.
    private Set<String> reservedNames;

    private UserManager() {
        userProfile = new ConcurrentHashMap<>(16);
        names = Collections.synchronizedSet(new HashSet<>());
        reservedNames = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Clears all user profiles.
     */
    public void clearAllProfiles() {
        userProfile.clear();
        names.clear();
    }

    /**
     * Query the user name whether exists or not.
     * 
     * @param name User name to query.
     * @return True if exists.
     */
    public boolean containsName(String name) {
        return names.contains(name);
    }

    /**
     * Query the user UUID whether exists or not.
     * 
     * @param name UUID to query.
     * @return True if exists.
     */
    public boolean containsUuid(String uuid) {
        return userProfile.containsKey(uuid);
    }

    /**
     * Delete the user profile corresponding to the UUID,
     * 
     * @param uuid The UUID.
     * @return True if delete successfully.
     */
    public User deleteUserProfile(String uuid) {
        names.remove(lookup(uuid).getName());
        return userProfile.remove(uuid);
    }

    /**
     * Obtain the user name corresponding to UUID.
     * 
     * @param uuid The UUID.
     * @return The corresponding user name, or null if UUID not exists.
     */
    public String getName(String uuid) {
        return lookup(uuid).getName();
    }

    /**
     * True if the user register profile is empty.
     * 
     * @return True if empty.
     */
    public boolean isEmptyUserProfile() {
        return userProfile.isEmpty();
    }

    /**
     * <p>
     * Obtain the read-only view of User object collection.
     * </p>
     * 
     * <p>
     * <b>Don't invoke remove() method of the iterator, it will cause exception.</b>
     * </p>
     */
    @Override
    public Iterator<User> iterator() {
        return Collections.unmodifiableCollection(userProfile.values()).iterator();
    }

    /**
     * Obtain the corresponding User object from UUID.
     * 
     * @param uuid UUID
     * @return The corresponding User object, or null if UUID not exists.
     */
    public User lookup(String uuid) {
        return userProfile.get(uuid);
    }

    /**
     * Create a new User object, and register it to the user profile list.
     * 
     * @param uuid    UUID
     * @param name    User name
     * @param channel Remote TCP socket channel
     */
    public void register(String uuid, String name, AsynchronousSocketChannel channel) {
        register(new User(uuid, channel, name));
    }

    /**
     * Register the specified User object to the user profile list.
     * 
     * @param u The User object.
     */
    public void register(User u) {
        userProfile.put(u.getUuid(), u);
        names.add(u.getName());
    }

    /**
     * Register user name, but not to associate with User object. (Reserve User
     * name, Intended for WebSocket Service.)
     * 
     * @param name The user name.
     * @return True if registered successfully.
     */
    public boolean reserveName(String name) {
        if (!names.contains(name) && !names.contains(name)) {
            return names.add(name) & reservedNames.add(name);
        }

        return false;
    }

    /**
     * Remove the reservation of user name.
     * 
     * @param name User name
     * @return Whether the removal is success
     */
    public boolean undoReserveName(String name) {
        if (reservedNames.contains(name) && names.contains(name)) {
            return reservedNames.remove(name) & names.remove(name);
        }

        return false;
    }

    /**
     * Gather the String representation of registered user profile (TCP Server only).
     */
    @Override
    public String toString() {
        return userProfile.toString();
    }

    /**
     * Return the read-only view of the User object collection.  
     * 
     * @return Read-only User object collection.
     */
    public Collection<User> userProfileValueSet() {
        return Collections.unmodifiableCollection(userProfile.values());
    }
}
