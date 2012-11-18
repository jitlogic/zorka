/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ZorkaLibUnitTest extends ZorkaFixture {

    @Test
    public void testRegisterTwoNamesAndListOnlyNames() throws Exception {
        mBeanServerRegistry.getOrRegister("test", "test:type=ZorkaLib,name=test1", "a", "a");
        mBeanServerRegistry.getOrRegister("test", "test:type=ZorkaLib,name=test2", "a", "a");

        String[] s = zorkaLib.ls("test", "test:type=ZorkaLib,*").split("\n");

        assertEquals(2, s.length);
        assertEquals("test:type=ZorkaLib,name=test1", s[0]);
        assertEquals("test:type=ZorkaLib,name=test2", s[1]);
    }

    @Test
    public void testRegisterOneNameWithTwoAttributesAndListThem() throws Exception {
        mBeanServerRegistry.getOrRegister("test", "test:type=ZorkaLib,name=test1", "a", "aaa");
        mBeanServerRegistry.getOrRegister("test", "test:type=ZorkaLib,name=test1", "b", "bbb");

        String[] s = zorkaLib.ls("test", "test:type=ZorkaLib,*", "*").split("\n");

        assertEquals(2, s.length);

        assertEquals("test:type=ZorkaLib,name=test1: a -> aaa", s[0]);
    }

    @Test
    public void testRegisterAndListKeyValueHashMap() throws Exception {
        Map<String,String> map = new HashMap<String, String>();
        map.put("a", "aaa"); map.put("b", "bbb");
        mBeanServerRegistry.getOrRegister("test", "test:type=ZorkaLib,name=test1", "map", map);

        String[] s = zorkaLib.ls("test", "test:type=ZorkaLib,*", "map").split("\n");

        assertEquals(2, s.length);
        assertEquals("test:type=ZorkaLib,name=test1: map.a -> aaa", s[0]);
        assertEquals("test:type=ZorkaLib,name=test1: map.b -> bbb", s[1]);
    }
}
