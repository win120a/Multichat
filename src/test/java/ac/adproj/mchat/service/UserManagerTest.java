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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private static UserManager userManager;

    private static String testUserName;

    private String uuid;

    @BeforeAll
    static void setUp() {
        userManager = UserManager.getInstance();
    }

    @AfterEach
    void autoCleanup() {
        userManager.clearAllProfiles();
    }

    @BeforeEach
    void autoRegisterRandomGeneratedUser() {
        uuid = UUID.randomUUID().toString();
        testUserName = "TEST" + UUID.randomUUID();

        userManager.register(uuid, testUserName, null);
    }

    @Test
    void testRegister() {
        assertFalse(userManager.isEmptyUserProfile());
        assertTrue(userManager.containsUuid(uuid));

        assertTrue(userManager.register(UUID.randomUUID().toString(), "N_" + UUID.randomUUID(), null));

        assertFalse(userManager.register(uuid, "N_" + UUID.randomUUID(), null));
    }

    @Test
    void clearAllProfiles() {
        userManager.clearAllProfiles();

        assertTrue(userManager.isEmptyUserProfile());
        assertFalse(userManager.containsUuid(uuid));
    }

    @Test
    void containsName() {
        assertTrue(userManager.containsName(testUserName));
    }

    @Test
    void containsUuid() {
        assertTrue(userManager.containsUuid(uuid));
    }

    @Test
    void deleteUserProfile() {
        userManager.deleteUserProfile(uuid);
        assertFalse(userManager.containsUuid(uuid));
    }

    @Test
    void getName() {
        assertEquals(testUserName, userManager.getName(uuid));
    }

    @Test
    void isEmptyUserProfile() {
        assertFalse(userManager.isEmptyUserProfile());
        clearAllProfiles();
        assertTrue(userManager.isEmptyUserProfile());
    }

    @Test
    void lookup() {
        assertEquals(testUserName, userManager.lookup(uuid).getName());
        assertEquals(uuid, userManager.lookup(uuid).getUuid());
    }

    @Test
    void testReserveAndUndoReserveName() {
        var theSecondName = "T2_" + UUID.randomUUID();
        assertTrue(userManager.reserveName(theSecondName));

        // Check return value if the name is already reserved.
        assertFalse(userManager.reserveName(theSecondName));

        // Register another user with the same name (it should be failed).
        var uuid2 = UUID.randomUUID().toString();

        // It should be failed when the name is reserved.
        assertFalse(userManager.register(uuid2, theSecondName, null));
        assertFalse(userManager.containsUuid(uuid2));

        // Check the registration result when the reservation revoked.
        assertTrue(userManager.undoReserveName(theSecondName));

        // It should return false when the name become free.
        assertFalse(userManager.undoReserveName(theSecondName));

        // It should be successful.
        assertTrue(userManager.register(uuid2, theSecondName, null));
        assertTrue(userManager.containsUuid(uuid2));
    }

    @Test
    void findUuidByName() {
        var optionalOfUUID = userManager.findUuidByName(testUserName);

        if (optionalOfUUID.isPresent()) {
            assertEquals(uuid, optionalOfUUID.get());
        } else {
            fail("The corresponding user is not exist! " +
                    "[Expected UUID: " + uuid + ", Expected UserName: " + testUserName + "]");
        }
    }

    @Test
    void userProfileValueSet() {
        var valueSet = userManager.userProfileValueSet();

        // Normally there's only one item.
        for (var i : valueSet) {
            assertEquals(uuid, i.getUuid());
            assertEquals(testUserName, i.getName());
        }
    }
}