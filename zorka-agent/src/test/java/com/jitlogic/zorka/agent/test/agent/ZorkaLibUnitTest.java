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
package com.jitlogic.zorka.agent.test.agent;

import com.jitlogic.zorka.agent.test.support.ZorkaFixture;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ZorkaLibUnitTest extends ZorkaFixture {

    @Test
    public void testRegisterTwoNamesAndListOnlyNames() throws Exception {
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test1", "a", "a");
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test2", "a", "a");

        String[] s = zorka.ls("test", "test:type=ZorkaLib,*").split("\n");

        assertEquals(2, s.length);
        assertEquals("test:type=ZorkaLib,name=test1", s[0]);
        assertEquals("test:type=ZorkaLib,name=test2", s[1]);
    }

    @Test
    public void testRegisterOneNameWithTwoAttributesAndListThem() throws Exception {
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test1", "a", "aaa");
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test1", "b", "bbb");

        String[] s = zorka.ls("test", "test:type=ZorkaLib,*", "*").split("\n");

        assertEquals(2, s.length);

        assertEquals("test:type=ZorkaLib,name=test1: a -> aaa", s[0]);
    }

    @Test
    public void testRegisterAndListKeyValueHashMap() throws Exception {
        Map<String,String> map = new HashMap<String, String>();
        map.put("a", "aaa"); map.put("b", "bbb");
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test1", "map", map, "Some map.");

        String[] s = zorka.ls("test", "test:type=ZorkaLib,*", "map").split("\n");

        assertEquals(2, s.length);
        assertEquals("test:type=ZorkaLib,name=test1: map.a -> aaa", s[0]);
        assertEquals("test:type=ZorkaLib,name=test1: map.b -> bbb", s[1]);
    }

    @Test
    public void testRegisterAndListListItems() throws Exception {
        List<String> lst = Arrays.asList("aaa", "bbb");
        zorka.registerAttr("test", "test:type=ZorkaLib,name=test1", "lst", lst, "Some list.");

        String[] s = zorka.ls("test", "test:type=ZorkaLib,*", "lst").split("\n");

        assertEquals(2, s.length);
        assertEquals("test:type=ZorkaLib,name=test1: lst.0 -> aaa", s[0]);
        assertEquals("test:type=ZorkaLib,name=test1: lst.1 -> bbb", s[1]);
    }
}
