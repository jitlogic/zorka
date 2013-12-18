/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.test.support.ZicoFixture;

import com.jitlogic.zorka.common.test.support.TestUtil;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class HostManagementApiUnitTest extends ZicoFixture {


    @Test
    public void testCreateEmptyHostAndCheckHostDir() throws Exception {
        assertNull(hostStoreManager.getHost("test", false));
        assertEquals(0, hostService.findAll().size());

        assertNotNull(hostStoreManager.getHost("test", true));
        assertEquals(1, hostService.findAll().size());

        HostStore host = hostStoreManager.getHost("test", false);
        assertTrue(new File(host.getRootPath(), "host.properties").exists());

        host.setAddr("1.2.3.4"); host.save();
        assertEquals("1.2.3.4",
            TestUtil.loadProps(new File(host.getRootPath(), "host.properties").getPath()).getProperty("addr"));

    }

    @Test
    public void testCreateRemoveHost() throws Exception {
        assertNotNull(hostStoreManager.getHost("test", true));
        HostStore host = hostStoreManager.getHost("test", false);
        hostStoreManager.delete("test");
        assertFalse(new File(host.getRootPath(), "host.properties").exists());
        assertNull(hostStoreManager.getHost("test", false));
        assertEquals(0, hostService.findAll().size());
    }

    //
}
