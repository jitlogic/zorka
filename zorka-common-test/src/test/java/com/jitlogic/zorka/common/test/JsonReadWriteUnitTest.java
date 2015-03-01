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

package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.util.JSONReader;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonReadWriteUnitTest {

    public static Object parse(String json) {
        return new JSONReader().read(json);
    }

    public static String unparse(Object obj) {
        return new JSONWriter(false).write(obj);
    }

    public static List lst(Object...obj) {
        List rslt = new ArrayList(obj.length);
        rslt.addAll(Arrays.asList(obj));
        return rslt;
    }

    public static Map map(Object...objs){
        return ZorkaUtil.map(objs);
    }

    @Test
    public void simpleDataParseTest() {
        assertEquals(1L, parse("1"));
        assertEquals(1.23, parse("1.23"));
        assertEquals("abc", parse("\"abc\""));
    }

    @Test
    public void simpleDataUnparseTest() {
        assertEquals("1", unparse(1));
        assertEquals("1.23", unparse(1.23));
        assertEquals("\"abc\"", unparse("abc"));
    }

    @Test
    public void structuredDataParseTest() {
        assertEquals(lst(1L,2L,3L), parse("[1,2,3]"));
        assertEquals(lst("a","b","c"), parse("[\"a\",\"b\",\"c\"]"));
        assertEquals(map("a", 1L, "b", 2L), parse("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void structuredDataUnparseTest() {
        assertEquals("[1,2,3]", unparse(lst(1,2,3)));
        assertEquals("[\"a\",\"b\",\"c\"]", unparse(lst("a","b","c")));
    }

    // TODO typed serialization & deserialization
}

