/*
 * Copyright (C) 2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.organization;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;

public class TestUserHandler extends TestOrganization {

    private MyUserEventListener listener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        listener = new MyUserEventListener();
        userHandler_.addUserEventListener(listener);
    }

    @Override
    public void tearDown() throws Exception {
        userHandler_.removeUserEventListener(listener);
        super.tearDown();
    }

    public void testAuthenticate() throws Exception {
        // authentication with existing user and correct password
        assertTrue(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));

        // unknown user authentication
        assertFalse(userHandler_.authenticate(USER_1 + "_", DEFAULT_PASSWORD));

        // authentication with wrong password
        assertFalse(userHandler_.authenticate(USER_1, "_"));

        boolean unsupportedOperation = false;
        try {
            // Disable the user testAuthenticate
            userHandler_.setEnabled(USER_1, false, true);

            try {
                userHandler_.authenticate(USER_1, DEFAULT_PASSWORD);
                fail("A DisabledUserException was expected");
            } catch (DisabledUserException e) {
                // expected exception
            }

            // Enable the user testAuthenticate
            userHandler_.setEnabled(USER_1, true, true);
            assertTrue(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user testAuthenticate
        userHandler_.removeUser(USER_1, true);

        // The user testAuthenticate doesn't exist anymore thus the authentication should fail
        assertFalse(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));

        // Check the listener's counters
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testFindUserByName() throws Exception {
        // try to find existed user
        User u = userHandler_.findUserByName("demo");

        assertNotNull(u);
        assertEquals("demo@localhost", u.getEmail());
        assertEquals("Demo", u.getFirstName());
        assertEquals("gtn", u.getLastName());
        assertEquals("demo", u.getUserName());
        assertTrue(u.isEnabled());

        // try to find a non existing user. We are supposed to get "null" instead of Exception.
        try {
            assertNull(userHandler_.findUserByName("not-existed-user"));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
        try {
            assertNull(userHandler_.findUserByName("not-existed-user", true));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
        try {
            assertNull(userHandler_.findUserByName("not-existed-user", false));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }

        assertNotNull(userHandler_.findUserByName(USER_1));
        assertTrue(userHandler_.findUserByName(USER_1).isEnabled());
        assertNotNull(userHandler_.findUserByName(USER_1, true));
        assertTrue(userHandler_.findUserByName(USER_1, true).isEnabled());
        assertNotNull(userHandler_.findUserByName(USER_1, false));
        assertTrue(userHandler_.findUserByName(USER_1, false).isEnabled());

        boolean unsupportedOperation = false;
        try {
            // Disable the user testFindUserByName
            userHandler_.setEnabled(USER_1, false, true);

            // We should not find the user testFindUserByName anymore from the normal method
            assertNull(userHandler_.findUserByName(USER_1));
            assertNull(userHandler_.findUserByName(USER_1, true));
            // We should find it using the method that includes the disabled user account
            assertNotNull(userHandler_.findUserByName(USER_1, false));
            assertFalse(userHandler_.findUserByName(USER_1, false).isEnabled());

            // Enable the user testFindUserByName
            userHandler_.setEnabled(USER_1, true, true);

            // We should find it again whatever the value of the parameter enabledOnly
            assertNotNull(userHandler_.findUserByName(USER_1));
            assertTrue(userHandler_.findUserByName(USER_1).isEnabled());
            assertNotNull(userHandler_.findUserByName(USER_1, true));
            assertTrue(userHandler_.findUserByName(USER_1, true).isEnabled());
            assertNotNull(userHandler_.findUserByName(USER_1, false));
            assertTrue(userHandler_.findUserByName(USER_1, false).isEnabled());
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user testFindUserByName
        userHandler_.removeUser(USER_1, true);

        // try to find a user that doesn't exist anymore. We are supposed to get "null" instead of Exception.
        try {
            assertNull(userHandler_.findUserByName(USER_1));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
        try {
            assertNull(userHandler_.findUserByName(USER_1, true));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
        try {
            assertNull(userHandler_.findUserByName(USER_1, false));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }

        // Check the listener's counters
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testFindUsersByQuery() throws Exception {
        createUser("tolik");
        userHandler_.authenticate("tolik", DEFAULT_PASSWORD);

        Query query = new Query();
        query.setEmail("tolik@exoportal.org");

        // try to find user by email
        assertSizeEquals(1, userHandler_.findUsersByQuery(query), true);
        assertSizeEquals(1, userHandler_.findUsersByQuery(query, true), true);
        assertSizeEquals(1, userHandler_.findUsersByQuery(query, false), false);

        // try to find user by name with mask
        query = new Query();
        query.setUserName("*tolik*");
        assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by name with mask
        query = new Query();
        query.setUserName("tol*");
        assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by name with mask
        query = new Query();
        query.setUserName("*lik");
        assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by name explicitly
        query = new Query();
        query.setUserName("tolik");
        assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by name explicitly, case sensitive search
        // query = new Query();
        // query.setUserName("Tolik");
        // assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by part of name without mask
        // query = new Query();
        // query.setUserName("tol");
        // assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        // try to find user by fist and last names, case sensitive search
        // query = new Query();
        // query.setFirstName("dEfaUlt");
        // query.setLastName("dEfaulT");
        // assertSizeEquals(1, userHandler_.findUsersByQuery(query));

        String skipDateTests = System.getProperty("orgservice.test.configuration.skipDateTests");
        if (!"true".equals(skipDateTests)) {
            // try to find user by login date
            Calendar calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

            query = new Query();
            query.setFromLoginDate(calc.getTime());
            query.setUserName("tolik");
            assertSizeEquals(1, userHandler_.findUsersByQuery(query));

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

            // query = new Query();
            // query.setFromLoginDate(calc.getTime());
            // assertSizeEquals(0, userHandler_.findUsersByQuery(query));

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

            // query = new Query();
            // query.setToLoginDate(calc.getTime());
            // assertSizeEquals(0, userHandler_.findUsersByQuery(query));

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

            query = new Query();
            query.setToLoginDate(calc.getTime());
            query.setUserName("tolik");
            assertSizeEquals(1, userHandler_.findUsersByQuery(query));
        }

        createUser("rolik");
        createUser("bolik");
        createUser("volik");

        query = new Query();
        // query.setUserName("olik");
        query.setUserName("*olik");

        ListAccess<User> users = userHandler_.findUsersByQuery(query);

        assertSizeEquals(4, users, true);
        assertSizeEquals(4, userHandler_.findUsersByQuery(query, true), true);
        assertSizeEquals(4, userHandler_.findUsersByQuery(query, false), false);

        User[] allPage = users.load(0, 4);
        User[] page1 = users.load(0, 2);
        User[] page2 = users.load(2, 2);

        assertEquals(allPage[0].getUserName(), page1[0].getUserName());
        assertEquals(allPage[1].getUserName(), page1[1].getUserName());
        assertEquals(allPage[2].getUserName(), page2[0].getUserName());
        assertEquals(allPage[3].getUserName(), page2[1].getUserName());

        try {
            users.load(0, 0);
        } catch (Exception e) {
            fail("Exception is not expected");
        }

        // try to load more than exist
        try {
            users.load(0, 5);
            fail("Exception is expected");
        } catch (Exception e) {
        }

        // try to load more than exist
        try {
            users.load(1, 4);
            fail("Exception is expected");
        } catch (Exception e) {
        }

        boolean unsupportedOperation = false;
        try {
            // Disable the user tolik
            userHandler_.setEnabled("tolik", false, true);

            assertSizeEquals(3, userHandler_.findUsersByQuery(query), true);
            assertSizeEquals(3, userHandler_.findUsersByQuery(query, true), true);
            assertSizeEquals(4, userHandler_.findUsersByQuery(query, false), false);

            // Enable the user tolik
            userHandler_.setEnabled("tolik", true, true);

            assertSizeEquals(4, userHandler_.findUsersByQuery(query), true);
            assertSizeEquals(4, userHandler_.findUsersByQuery(query, true), true);
            assertSizeEquals(4, userHandler_.findUsersByQuery(query, false), false);
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user tolik
        userHandler_.removeUser("tolik", true);
        userHandler_.removeUser("rolik", true);
        userHandler_.removeUser("bolik", true);
        userHandler_.removeUser("volik", true);

        assertSizeEquals(0, userHandler_.findUsersByQuery(query), true);
        assertSizeEquals(0, userHandler_.findUsersByQuery(query, true), true);
        assertSizeEquals(0, userHandler_.findUsersByQuery(query, false), false);

        // Check the listener's counters
        assertEquals(4, listener.preSaveNew);
        assertEquals(4, listener.postSaveNew);
        assertEquals(0, listener.preSave);
        assertEquals(0, listener.postSave);
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(4, listener.preDelete);
        assertEquals(4, listener.postDelete);
    }

    public void testFindUsers() throws Exception {
        userHandler_.authenticate(USER_1, DEFAULT_PASSWORD);

        Query query = new Query();
        query.setEmail(USER_1 + "@exoportal.org");

        // try to find user by email
        assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by name with mask
        query = new Query();
        query.setUserName("*testOrganization_user1*");
        assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by name with mask
        query = new Query();
        query.setUserName("*_user1");
        assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by name with mask
        query = new Query();
        query.setUserName("testOrganization_*");
        assertSizeEquals(3, userHandler_.findUsers(query).getAll());

        // try to find user by name explicitly
        query = new Query();
        query.setUserName(USER_1);
        assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by name explicitly, case sensitive search
        // query = new Query();
        // query.setUserName("Tolik");
        // assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by part of name without mask
        // query = new Query();
        // query.setUserName("tol");
        // assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        // try to find user by fist and last names, case sensitive search
        // query = new Query();
        // query.setFirstName("fiRst");
        // query.setLastName("lasT");
        // assertSizeEquals(1, userHandler_.findUsers(query).getAll());

        String skipDateTests = System.getProperty("orgservice.test.configuration.skipDateTests");
        if (!"true".equals(skipDateTests)) {
            // try to find user by login date
            Calendar calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

            query = new Query();
            query.setFromLoginDate(calc.getTime());
            query.setUserName(USER_1);
            assertSizeEquals(1, userHandler_.findUsers(query).getAll());

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

            // query = new Query();
            // query.setFromLoginDate(calc.getTime());
            // assertSizeEquals(0, userHandler_.findUsers(query).getAll());

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

            // query = new Query();
            // query.setToLoginDate(calc.getTime());
            // assertSizeEquals(0, userHandler_.findUsers(query).getAll());

            calc = Calendar.getInstance();
            calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

            query = new Query();
            query.setToLoginDate(calc.getTime());
            query.setUserName(USER_1);
            assertSizeEquals(1, userHandler_.findUsers(query).getAll());
        }
    }

    public void testGetUserPageList() throws Exception {
        assertSizeEquals(8, userHandler_.getUserPageList(10).getAll());
    }

    public void testFindAllUsers() throws Exception {
        assertSizeEquals(8, userHandler_.findAllUsers(), true);
        assertSizeEquals(8, userHandler_.findAllUsers(true), true);
        assertSizeEquals(8, userHandler_.findAllUsers(false), false);

        ListAccess<User> users = userHandler_.findAllUsers();
        User[] allPage = users.load(0, 8);
        User[] page1 = users.load(0, 2);
        User[] page2 = users.load(2, 2);

        assertEquals(allPage[0].getUserName(), page1[0].getUserName());
        assertEquals(allPage[1].getUserName(), page1[1].getUserName());
        assertEquals(allPage[2].getUserName(), page2[0].getUserName());
        assertEquals(allPage[3].getUserName(), page2[1].getUserName());

        // try {
        // users.load(0, 0);
        // } catch (Exception e) {
        // fail("Exception is not expected");
        // }

        // try to load more than exist
        // Due to duplicate user problem with pickletlink run with DB + LDAP
        // We don't support throw exception on this case
        // try {
        // users.load(0, 9);
        // fail("Exception is expected");
        // } catch (Exception e) {
        // }

        // try to load more than exist
        // try {
        // users.load(1, 8);
        // fail("Exception is expected");
        // } catch (Exception e) {
        // }

        String userName = USER_1;

        boolean unsupportedOperation = false;
        try {
            // Disable the user
            userHandler_.setEnabled(userName, false, true);

            // IDMUserListAcess#getSize() can't filter disabled user for now
            // wait for PersistenceManager.getUserCount(true)
            // assertSizeEquals(7, userHandler_.findAllUsers(), true);
            // assertSizeEquals(7, userHandler_.findAllUsers(true), true);
            assertSizeEquals(8, userHandler_.findAllUsers(false), false);

            // Enable the user
            userHandler_.setEnabled(userName, true, true);

            assertSizeEquals(8, userHandler_.findAllUsers(), true);
            assertSizeEquals(8, userHandler_.findAllUsers(true), true);
            assertSizeEquals(8, userHandler_.findAllUsers(false), false);
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user
        userHandler_.removeUser(userName, true);

        assertSizeEquals(7, userHandler_.findAllUsers(), true);
        assertSizeEquals(7, userHandler_.findAllUsers(true), true);
        assertSizeEquals(7, userHandler_.findAllUsers(false), false);

        // Check the listener's counters
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testRemoveUser() throws Exception {
        assertEquals("We expect to find single membership for user " + USER_1, 1,
                membershipHandler_.findMembershipsByUser(USER_1).size());

        assertNotNull(userHandler_.removeUser(USER_1, true));

        assertNull(profileHandler_.findUserProfileByName(USER_1));
        assertEquals("We expect to find no membership for user " + USER_1, 0,
                membershipHandler_.findMembershipsByUser(USER_1).size());

        // try to find user after remove. We are supposed to get "null" instead of exception
        try {
            assertNull(userHandler_.findUserByName(USER_1));
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    public void testSaveUser() throws Exception {
        String userName = USER_1;

        String newEmail = "new@Email";
        String displayName = "name";

        // change email and check
        User u = userHandler_.findUserByName(userName);
        u.setEmail(newEmail);

        userHandler_.saveUser(u, true);

        u = userHandler_.findUserByName(userName);
        assertEquals(newEmail, u.getEmail());
        assertEquals(u.getDisplayName(), u.getFirstName() + " " + u.getLastName());

        u.setDisplayName(displayName);
        userHandler_.saveUser(u, true);

        u = userHandler_.findUserByName(userName);
        assertEquals(displayName, u.getDisplayName());

        boolean unsupportedOperation = false;
        try {
            // Disable the user
            u = userHandler_.setEnabled(userName, false, true);
            u.setDisplayName(displayName + "new-value");
            try {
                userHandler_.saveUser(u, true);
                fail("A DisabledUserException was expected");
            } catch (DisabledUserException e) {
                // expected issue
            }

            // Enable the user
            u = userHandler_.setEnabled(userName, true, true);
            u.setDisplayName(displayName + "new-value");
            userHandler_.saveUser(u, true);

            u = userHandler_.findUserByName(userName);
            assertEquals(displayName + "new-value", u.getDisplayName());
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user
        userHandler_.removeUser(userName, true);

        // Check the listener's counters;
        assertEquals(unsupportedOperation ? 2 : 3, listener.preSave);
        assertEquals(unsupportedOperation ? 2 : 3, listener.postSave);
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testChangePassword() throws Exception {
        // authentication with existing user and correct password
        assertTrue(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));

        // authentication with wrong password
        assertFalse(userHandler_.authenticate(USER_1, ""));

        User u = userHandler_.findUserByName(USER_1);
        u.setPassword(DEFAULT_PASSWORD + "_");
        userHandler_.saveUser(u, true);

        // authentication with existing user and correct password
        assertTrue(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD + "_"));

        // authentication with wrong password
        assertFalse(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));

        boolean unsupportedOperation = false;
        try {
            // Disable the user
            u = userHandler_.setEnabled(USER_1, false, true);
            u.setPassword(DEFAULT_PASSWORD);

            try {
                userHandler_.saveUser(u, true);
                fail("A DisabledUserException was expected");
            } catch (DisabledUserException e) {
                // expected issue
            }

            try {
                // authentication with existing user and correct password
                userHandler_.authenticate(USER_1, DEFAULT_PASSWORD + "_");
                fail("A DisabledUserException was expected");
            } catch (DisabledUserException e) {
                // expected issue
            }

            try {
                // authentication with wrong password
                userHandler_.authenticate(USER_1, DEFAULT_PASSWORD);
                fail("A DisabledUserException was expected");
            } catch (DisabledUserException e) {
                // expected issue
            }

            // Disable the user
            u = userHandler_.setEnabled(USER_1, true, true);
            u.setPassword(DEFAULT_PASSWORD);
            userHandler_.saveUser(u, true);

            // authentication with existing user and correct password
            assertTrue(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD));

            // authentication with wrong password
            assertFalse(userHandler_.authenticate(USER_1, DEFAULT_PASSWORD + "_"));
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user
        userHandler_.removeUser(USER_1, true);

        // Check the listener's counters
        assertEquals(unsupportedOperation ? 1 : 2, listener.preSave);
        assertEquals(unsupportedOperation ? 1 : 2, listener.postSave);
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testCreateUser() throws Exception {
        String userName = "foo";
        User u = userHandler_.createUserInstance(userName);
        u.setEmail("email@test");
        u.setFirstName("first");
        u.setLastName("last");
        u.setPassword(DEFAULT_PASSWORD);
        userHandler_.createUser(u, true);

        // check if user exists
        assertNotNull(userHandler_.findUserByName(userName));
        
        userHandler_.removeUser(userName, true);

        // Check the listener's counters
        assertEquals(1, listener.preSaveNew);
        assertEquals(1, listener.postSaveNew);
        assertEquals(0, listener.preSave);
        assertEquals(0, listener.postSave);
        assertEquals(0, listener.preSetEnabled);
        assertEquals(0, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    public void testFindUsersByGroupId() throws Exception {
        String groupId = "/platform/users";
        assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId), true);
        assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId, true), true);
        assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId, false), false);

        boolean unsupportedOperation = false;
        try {
            // Disable the user
            userHandler_.setEnabled(USER_1, false, true);

            assertSizeEquals(7, userHandler_.findUsersByGroupId(groupId), true);
            assertSizeEquals(7, userHandler_.findUsersByGroupId(groupId, true), true);
            assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId, false), false);

            // Enable the user
            userHandler_.setEnabled(USER_1, true, true);

            assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId), true);
            assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId, true), true);
            assertSizeEquals(8, userHandler_.findUsersByGroupId(groupId, false), false);
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported
            unsupportedOperation = true;
        }

        // Remove the user
        userHandler_.removeUser(USER_1, true);

        assertSizeEquals(7, userHandler_.findUsersByGroupId(groupId), true);
        assertSizeEquals(7, userHandler_.findUsersByGroupId(groupId, true), true);
        assertSizeEquals(7, userHandler_.findUsersByGroupId(groupId, false), false);

        // Check the listener's counters
        assertEquals(unsupportedOperation ? 0 : 2, listener.preSetEnabled);
        assertEquals(unsupportedOperation ? 0 : 2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }
       
    public void testGetListeners() throws Exception {
        if (userHandler_ instanceof UserEventListenerHandler) {
            List<UserEventListener> list = ((UserEventListenerHandler) userHandler_).getUserListeners();
            try {
                // check if we able to modify the list of listeners
                list.clear();
                fail("Exception should not be thrown");
            } catch (Exception e) {
            }
        }
    }

    public void testSetEnabled() throws Exception {
        try {
            // Trying to disable a non existing user should not throw any exception
            assertNull(userHandler_.setEnabled("foo", false, true));
        } catch (UnsupportedOperationException e) {
            // This operation can be unsupported, the unit test will be ignored
            return;
        }
        
        // Trying to disable an existing user should return the corresponding user
        User user = userHandler_.setEnabled(USER_1, false, true);

        assertNotNull(user);
        assertEquals(USER_1, user.getUserName());
        assertFalse(user.isEnabled());

        // Trying to disable an user already disabled
        user = userHandler_.setEnabled(USER_1, false, true);

        assertNotNull(user);
        assertEquals(USER_1, user.getUserName());
        assertFalse(user.isEnabled());

        // Trying to enable the user
        user = userHandler_.setEnabled(USER_1, true, true);

        assertNotNull(user);
        assertEquals(USER_1, user.getUserName());
        assertTrue(user.isEnabled());

        // Trying to enable an user already enabled
        user = userHandler_.setEnabled(USER_1, true, true);

        assertNotNull(user);
        assertEquals(USER_1, user.getUserName());
        assertTrue(user.isEnabled());

        // Remove the user testSetEnabled
        userHandler_.removeUser(USER_1, true);
        assertNull(userHandler_.setEnabled(USER_1, false, true));

        // Check the listener's counters
        assertEquals(2, listener.preSetEnabled);
        assertEquals(2, listener.postSetEnabled);
        assertEquals(1, listener.preDelete);
        assertEquals(1, listener.postDelete);
    }

    protected void assertSizeEquals(int expectedSize, ListAccess<?> list) throws Exception {
        int size;
        assertEquals(expectedSize, size = list.getSize());
        Object[] values = list.load(0, size);
        size = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                size++;
            }
        }
        assertEquals(expectedSize, size);
    }

    protected void assertSizeEquals(int expectedSize, Collection<?> list) throws Exception {
        int size;
        assertEquals(expectedSize, size = list.size());
        size = 0;
        for (Object value : list) {
            if (value != null) {
                size++;
            }
        }
        assertEquals(expectedSize, size);
    }

    protected void assertSizeEquals(int expectedSize, ListAccess<User> list, boolean enabledOnly) throws Exception {
        int size;
        assertEquals(expectedSize, size = list.getSize());
        User[] values = list.load(0, size);
        size = 0;
        for (int i = 0; i < values.length; i++) {
            User usr = values[i];
            if (usr != null && (!enabledOnly || usr.isEnabled())) {
                size++;
            }
        }
        assertEquals(expectedSize, size);
    }

    private static class MyUserEventListener extends UserEventListener {
        public int preSaveNew, postSaveNew;
        public int preSave, postSave;
        public int preDelete, postDelete;
        public int preSetEnabled, postSetEnabled;

        @Override
        public void preSave(User user, boolean isNew) throws Exception {
            if (user == null)
                return;
            if (isNew)
                preSaveNew++;
            else
                preSave++;
        }

        @Override
        public void postSave(User user, boolean isNew) throws Exception {
            if (user == null)
                return;
            if (isNew)
                postSaveNew++;
            else
                postSave++;
        }

        @Override
        public void preDelete(User user) throws Exception {
            if (user == null)
                return;
            preDelete++;
        }

        @Override
        public void postDelete(User user) throws Exception {
            if (user == null)
                return;
            postDelete++;
        }

        @Override
        public void preSetEnabled(User user) throws Exception {
            if (user == null)
                return;
            preSetEnabled++;
        }

        @Override
        public void postSetEnabled(User user) throws Exception {
            if (user == null)
                return;
            postSetEnabled++;
        }
    }
}
