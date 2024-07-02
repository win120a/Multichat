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

package ac.adproj.mchat.service;

import ac.adproj.mchat.model.User;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * User Manager.
 * </p>
 *
 * <p>
 * Intended to share user registration information between TCP server & WebSocket
 * Server.
 * </p>
 *
 * <p>
 * <b>Implementation Note: Although usernames are shared between TCP and
 * WebSocket server,</b> <b>the user information of WebSocket Server isn't
 * managed by this class.</b><br />
 * </p>
 *
 * @author Andy Cheung
 * @since 2020.5.24
 */
public class UserManager implements Iterable<User> {


    private final Map<String, User> userProfile;

    private final Set<String> names;

    /**
     * Storage for the reserved names.
     */
    private final Set<String> reservedNames;

    private UserManager() {
        userProfile = new ConcurrentHashMap<>(16);
        names = Collections.synchronizedSet(new HashSet<>());
        reservedNames = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Obtain the only instance.
     *
     * @return The instance.
     */
    public static UserManager getInstance() {
        return Holder.instance;
    }

    /**
     * Clears all user profiles.
     */
    public void clearAllProfiles() {
        userProfile.clear();
        names.clear();
    }

    /**
     * Query the username whether exists or not.
     *
     * @param name Username to query.
     * @return True if exists.
     */
    public boolean containsName(String name) {
        return names.contains(name);
    }

    /**
     * Query the user UUID whether exists or not.
     *
     * @param uuid UUID to query.
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
     * Obtain the username corresponding to UUID.
     *
     * @param uuid The UUID.
     * @return The corresponding username, or null if UUID not exists.
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
     * <b>The remove() method of the iterator is not supported.</b>
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
     * @param name    Username
     * @param channel Remote TCP socket channel
     */
    public boolean register(String uuid, String name, AsynchronousSocketChannel channel) {
        return register(new User(uuid, channel, name));
    }

    /**
     * Register the specified User object to the user profile list.
     *
     * @param u The User object.
     */
    public boolean register(User u) {
        if (!names.contains(u.getName())) {
            return userProfile.put(u.getUuid(), u) == null && names.add(u.getName());
        }

        return false;
    }

    /**
     * Register username, but not to associate with User object. (Reserve Username,
     * Intended for WebSocket Service.)
     *
     * @param name The username.
     * @return True if registered successfully.
     */
    public boolean reserveName(String name) {
        if (!names.contains(name) && !reservedNames.contains(name)) {
            return names.add(name) && reservedNames.add(name);
        }

        return false;
    }

    /**
     * Remove the reservation of username.
     *
     * @param name The username
     * @return Whether the removal is success
     */
    public boolean undoReserveName(String name) {
        if (reservedNames.contains(name) && names.contains(name)) {
            return reservedNames.remove(name) && names.remove(name);
        }

        return false;
    }

    public Optional<String> findUuidByName(String name) {
        var optionalOfEntry = userProfile.entrySet()
                .stream()
                .filter(x -> x.getValue().getName().equals(name))
                .findFirst();

        return optionalOfEntry.map(Map.Entry::getKey);
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

    /**
     * Holder of the instance.
     */
    private static class Holder {
        static UserManager instance = new UserManager();
    }
}
