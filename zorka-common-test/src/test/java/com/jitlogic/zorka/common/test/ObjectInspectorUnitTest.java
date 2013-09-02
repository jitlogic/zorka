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
package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.*;

public class ObjectInspectorUnitTest {

    @Test
    public void testSubstituteWithDefaultVal1() {
        assertEquals("XXX", ObjectInspector.substitute("${X:XXX}", new HashMap<String, Object>()));
        assertEquals("ABC", ObjectInspector.substitute("${X:XXX}", ZorkaUtil.<String, Object>map("X", "ABC")));
        assertEquals("XXX", ObjectInspector.substitute("${0:XXX}", new Object[]{null}));
        assertEquals("ABC", ObjectInspector.substitute("${0:XXX}", new Object[]{"ABC"}));
    }


    @Test
    public void testSubstituteWithLimit() {
        assertEquals("ABC", ObjectInspector.substitute("${X~3}", ZorkaUtil.<String, Object>map("X", "ABCDEF")));
        assertEquals("ABC", ObjectInspector.substitute("${0~3}", new Object[]{"ABCDEF"}));
    }


    @Test
    public void testSubstituteWithDefaultAndLimit() {
        assertEquals("AB.YY", ObjectInspector.substitute("${X~2:XX}.${Y~2:YY}",
                ZorkaUtil.<String, Object>map("X", "ABCD")));
        assertEquals("AB.YY", ObjectInspector.substitute("${0~2:XX}.${1~2:YY}", new Object[]{"ABCD", null}));
    }


    @Test
    public void testSubstitutePropWithLimit() {
        Properties props = new Properties();
        props.setProperty("X", "ABCDEF");
        assertEquals("ABC", ObjectInspector.substitute("${X~3}", props));
    }
}
