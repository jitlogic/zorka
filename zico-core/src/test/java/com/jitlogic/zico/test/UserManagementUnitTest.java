/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zico.core.model.User;
import com.jitlogic.zico.test.support.UserTestContext;
import com.jitlogic.zico.test.support.ZicoFixture;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

public class UserManagementUnitTest extends ZicoFixture {

    private UserManager userManager;


    @Before
    public void setUp() {
        userManager = injector.getInstance(UserManager.class);
    }


    public static User mkUser(String userName, String realName, String passwd, int flags, String...hosts) {
        User user = new User();

        user.setUserName(userName);
        user.setRealName(realName);
        user.setPassword(passwd);
        user.setFlags(flags);
        user.setAllowedHosts(Arrays.asList(hosts));

        return user;
    }


    @Test
    public void testCreateCheckAndModifyUser() {
        userService.persist(mkUser("test", "Test User", "1qaz2wsx", User.ADMIN_USER, "host1", "host2", "host3", "host4"));

        User user = userService.findUser("test");

        assertEquals("test", user.getUserName());
        assertEquals(Arrays.asList("host1", "host2", "host3", "host4"), user.getAllowedHosts());

        assertEquals(1, userService.findAll().size());
    }


    @Test
    public void testManageAllowedHosts() {
        userService.persist(mkUser("test", "Test User", "1qaz2wsx", User.ADMIN_USER, "host1", "host2", "host3", "host4"));

        assertEquals(4, userService.getAllowedHosts("test").size());
        assertEquals(0, userService.getAllowedHosts("plunk").size());

        userService.setAllowedHosts("test", Arrays.asList("a", "b", "c"));
        assertEquals(3, userService.getAllowedHosts("test").size());
    }


    @Test
    public void testChangeUserPasswordAsAdminAndThenAsOrdinaryUser() {
        userService.persist(mkUser("test", "Test User", "noPass", User.ADMIN_USER, "host1", "host2", "host3", "host4"));
        userService.resetPassword("test", null, "somePass");
        assertTrue(userService.findUser("test").getPassword().startsWith("MD5:"));

        userContext.isAdmin = false;
        userService.resetPassword(null, "somePass", "otherPass");
    }

    @Test(expected = ZicoRuntimeException.class)
    public void testChangeUserPassWithInvalidPassword() {
        userService.persist(mkUser("test", "Test User", "noPass", User.ADMIN_USER, "host1", "host2", "host3", "host4"));
        userService.resetPassword("test", null, "somePass");
        assertTrue(userService.findUser("test").getPassword().startsWith("MD5:"));

        userContext.isAdmin = false;
        userService.resetPassword(null, "wrongPass", "otherPass");
    }

    @Test
    public void testAddUserCloseReopenAndCheckIfItsStillThere() {
        userService.persist(mkUser("test", "Test User", "noPass", User.ADMIN_USER, "host1", "host2", "host3", "host4"));
        UserManager uman = injector.getInstance(UserManager.class);
        uman.close(); uman.open();
        assertNotNull("User account should persist across restarts", userService.findUser("test"));
    }


    @Test
    public void testIfUserDbPersistsAcrossRestarts() {
        userService.persist(mkUser("test", "Test User", "noPass", User.ADMIN_USER, "host1", "host2", "host3", "host4"));

        userManager.close();
        userManager.open();

        User user = userService.findUser("test");
        assertNotNull(user);
        assertEquals("noPass", user.getPassword());
        assertEquals(Arrays.asList("host1", "host2", "host3", "host4"), user.getAllowedHosts());
    }


    @Test
    public void testExportImportUserDb() {
        userService.persist(mkUser("test", "Test User", "noPass", User.ADMIN_USER, "host1", "host2", "host3", "host4"));
        userManager.export();
        userManager.close();

        assertTrue("export file should exist", new File(config.getConfDir(), "users.json").exists());

        for (String fname : new String[] { "users.db", "users.db.p", "users.db.t" }) {
            new File(config.getConfDir(), fname).delete();
        }

        userManager.open();

        User user = userService.findUser("test");
        assertNotNull(user);
        assertEquals("noPass", user.getPassword());
        assertEquals(Arrays.asList("host1", "host2", "host3", "host4"), user.getAllowedHosts());
    }

}
