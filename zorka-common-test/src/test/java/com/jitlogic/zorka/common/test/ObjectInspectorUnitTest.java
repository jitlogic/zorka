/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.test.support.*;
import com.jitlogic.zorka.common.util.JmxObject;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.management.ObjectName;
import javax.management.j2ee.statistics.TimeStatistic;
import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ObjectInspectorUnitTest extends CommonFixture {

    @Test
    public void testInspectStatic() {

        // Call specifically for methods or fields
        assertEquals(126, ObjectInspector.get(TestInspectorClass.class, "count()"));
        assertEquals(123, ObjectInspector.get(TestInspectorClass.class, ".count"));
        assertEquals(234, ObjectInspector.get(TestInspectorClass.class, ".priv"));

        // Automatic resolve
        assertEquals(125, ObjectInspector.get(TestInspectorClass.class, "count"));
    }


    @Test
    public void testInspectArray() {
        String[] array = {"c", "b", "a"};

        assertEquals("b", ObjectInspector.get(array, 1));
        assertEquals("c", ObjectInspector.get(array, 0));
    }


    @Test
    public void testInspectArrayLength() {
        String[] array = {"a", "b", "c"};
        assertEquals(3, ObjectInspector.get(array, "length"));
    }


    @Test
    public void testInspectByteArrayLength() {
        byte[] array = {1, 2, 3};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals((byte) 2, ObjectInspector.get(array, 1));
    }


    @Test
    public void testInspectIntArrayLength() {
        int[] array = {1, 2, 3};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals(2, ObjectInspector.get(array, 1));
    }


    @Test
    public void testInspectLongArrayLength() {
        long[] array = {1, 2, 3};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals(2L, ObjectInspector.get(array, 1));
    }

    @Test
    public void testInspectDoubleArrayLength() {
        double[] array = {1.0, 2.0, 3.0};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals(2.0, (Double) (ObjectInspector.get(array, 1)), 0.01);
    }

    @Test
    public void testInspectFloatArrayLength() {
        float[] array = {1, 2, 3};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals(2, (Float) (ObjectInspector.get(array, 1)), 0.01);
    }

    @Test
    public void testInspectShortArrayLength() {
        short[] array = {1, 2, 3};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals((short) 2, ObjectInspector.get(array, 1));
    }


    @Test
    public void testInspectCharArrayLength() {
        char[] array = {'A', 'B', 'C'};
        assertEquals(3, ObjectInspector.get(array, "length"));
        assertEquals('B', ObjectInspector.get(array, 1));
    }

    @Test
    public void testListArray() {
        String[] array = {"c", "b", "a"};

        assertEquals(Arrays.asList(0, 1, 2), ObjectInspector.list(array));
    }


    @Test
    public void testInspectMap() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 11);
        map.put("b", 22);
        map.put("c", 33);

        assertEquals(11, ObjectInspector.get(map, "a"));
        assertEquals(3, ObjectInspector.get(map, "size()"));
    }


    @Test
    public void testLisMap() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 11);
        map.put("b", 22);
        map.put("c", 33);

        assertEquals("sorted map keys", Arrays.asList("a", "b", "c"), ObjectInspector.list(map));
    }


    @Test
    public void testInspectList() {
        List<String> list = Arrays.asList("c", "b", "a");

        assertEquals("b", ObjectInspector.get(list, 1));
        assertEquals("c", ObjectInspector.get(list, 0));
    }

    @Test
    public void testInspectExceptionStackTrace() {
        Throwable e = new Exception().fillInStackTrace();

        Object o = ObjectInspector.get(e, "printStackTrace");

        assertTrue("should be a string", o instanceof String);
        assertTrue("should contain multiple lines", ((String) o).split("\n").length > 1);
    }

    @Test
    public void testListList() {
        List<String> list = Arrays.asList("c", "b", "a");

        assertEquals(Arrays.asList(0, 1, 2), ObjectInspector.list(list));
    }


    @Test
    public void testInspectJ2eeStats() throws Exception {
        TimeStatistic ts = (TimeStatistic) ObjectInspector.get(new TestStats(), "aaa");
        assertNotNull("didn't resolve anything", ts);
        assertEquals("stat name", "aaa", ts.getName());
    }


    @Test
    public void testListJ2eeStats() throws Exception {
        assertEquals(Arrays.asList("aaa", "bbb", "ccc"), ObjectInspector.list(new TestStats()));
    }


    @Test
    public void testInspectJmxObj() throws Exception {
        JmxObject jmxObject = mkJmxObject();

        assertEquals(100L, ObjectInspector.get(jmxObject, "Nom"));
    }


    @Test
    public void testListJmxObj() throws Exception {
        JmxObject jmxObject = mkJmxObject();

        assertEquals(Arrays.asList("Div", "Nom", "StrMap"), ObjectInspector.list(jmxObject));
    }


    private JmxObject mkJmxObject() throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(100);
        bean.setDiv(200);
        ObjectName on = new ObjectName("zorka.test:name=test");
        testMbs.registerMBean(bean, on);
        return new JmxObject(on, testMbs, null);
    }


    @Test
    public void testRecordSubstitutions() {
        Map<String, Object> rec = ZorkaUtil.map("E0", "123", "E1", "4567", "R0", "aaa", "R1", "bbb");

        assertEquals("123!", ObjectInspector.substitute("${E0}!", rec));
        assertEquals("aaa", ObjectInspector.substitute("${R0}", rec));
        assertEquals("4", ObjectInspector.substitute("${E1.length()}", rec));
    }


    @Test
    public void testSubstituteSpecialChars() {
        Map<String, Object> rec = ZorkaUtil.map("A", "a $ b");
        assertEquals("a $ b", ObjectInspector.substitute("${A}", rec));
    }


    @Test
    public void testInspectPrivateFieldInObjectWithSuperclass() throws Exception {
        byte[] b1 = "ABCD".getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(b1);

        byte[] b2 = ObjectInspector.get(bis, "buf");

        assertSame(b1, b2);
    }


    @Test
    public void testInspectPrivateFielfOfSuperclass() throws Exception {
        Properties props = props("AAA", "BBB");

        assertSame(props, ObjectInspector.get(new TestInspectorClass2(props), "props"));

    }


    private Properties props(String... kv) {
        Properties properties = new Properties();
        for (int i = 1; i < kv.length; i += 2) {
            properties.setProperty(kv[i - 1], kv[i]);
        }
        return properties;
    }


    @Test
    public void testSubstituteWithPropertyValues() {
        Properties props = props("zorka.home.dir", "/opt/zorka", "my.prop", "Oja!");

        assertEquals("Oja! Zorka found in /opt/zorka !",
                ObjectInspector.substitute("${my.prop} Zorka found in ${zorka.home.dir} !", props));

        assertEquals("This is ${non.existent}.",
                ObjectInspector.substitute("This is ${non.existent}.", props));

        assertEquals("This is by default !",
                ObjectInspector.substitute("This is ${fubar:by default} !", props));
    }

    @Test
    public void testGetClassName() {
        Assert.assertEquals("java.lang.String", ObjectInspector.get(String.class, "name"));
    }

    @Test
    public void testTrivialSubstitutions() {
        assertEquals("ab123cd", ObjectInspector.substitute("ab${0}cd", new Object[]{"123"}));
        assertEquals("3", ObjectInspector.substitute("${0.length()}", new Object[]{"123"}));
    }


    @Test
    public void testClassNameGet() {
        String str = "oja!";

        Object obj1 = ObjectInspector.get(str, "class");
        Object obj2 = ObjectInspector.get(obj1, "name");

        assertEquals("java.lang.String", obj2);
    }


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

    @Test
    public void testSubstituteWithAlternatives() {
        assertEquals("ABC", ObjectInspector.substitute("${X|Y|Z}",
                ZorkaUtil.<String, Object>map("X", "ABC")));

        assertEquals("DEF", ObjectInspector.substitute("${X|Y|Z}",
                ZorkaUtil.<String, Object>map("Y", "DEF", "Z", "GHI")));

        assertEquals("GHI", ObjectInspector.substitute("${X|Y|Z}",
                ZorkaUtil.<String, Object>map("Z", "GHI")));

        assertEquals("JKL", ObjectInspector.substitute("${X|Y.A|Z}",
                ZorkaUtil.<String, Object>map("Y", ZorkaUtil.map("A", "JKL"))));

        assertEquals("ABC", ObjectInspector.substitute("${0|1}", new Object[]{null, "ABC"}));
    }

    @Test
    public void testSubstituteWithAlternativeProps() {

        Properties props = new Properties();
        props.setProperty("Y", "ABC");

        assertEquals("ABC", ObjectInspector.substitute("${X|Y|Z}", props));
    }


    // TODO tests for tabular data

}
