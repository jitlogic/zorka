package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.testutil.*;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

import org.junit.Before;
import org.junit.Test;

import javax.management.*;
import javax.management.j2ee.statistics.TimeStatistic;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspectorUnitTest extends ZorkaFixture {

    @Before
    public void initLocal() {
    }


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
        String[] array = { "c", "b", "a" };

        assertEquals("b", ObjectInspector.get(array, 1));
        assertEquals("c", ObjectInspector.get(array, 0));
    }


    @Test
    public void testListArray() {
        String[] array = { "c", "b", "a" };

        assertEquals(Arrays.asList(0,1,2), ObjectInspector.list(array));
    }


    @Test
    public void testInspectMap() {
        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 11); map.put("b", 22); map.put("c", 33);

        assertEquals(11, ObjectInspector.get(map, "a"));
        assertEquals(3, ObjectInspector.get(map, "size()"));
    }


    @Test
    public void testLisMap() {
        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 11); map.put("b", 22); map.put("c", 33);

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
        assertTrue("should contain multiple lines", ((String)o).split("\n").length > 1);
    }

    @Test
    public void testListList() {
        List<String> list = Arrays.asList("c", "b", "a");

        assertEquals(Arrays.asList(0,1,2), ObjectInspector.list(list));
    }


    @Test
    public void testInspectJ2eeStats() throws Exception {
        TimeStatistic ts = (TimeStatistic)ObjectInspector.get(new TestStats(), "aaa");
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


    @Test
    public void testTrivialSubstitutions() {
        assertEquals("ab123cd", ObjectInspector.substitute("ab${0}cd", new Object[] { "123" }));
        assertEquals("3", ObjectInspector.substitute("${0.length()}", new Object[] { "123"}));
    }


    @Test
    public void testClassNameGet() {
        String str = "oja!";

        Object obj1 = ObjectInspector.get(str, "class");
        Object obj2 = ObjectInspector.get(obj1, "name");

        assertEquals("java.lang.String", obj2);
    }



    // TODO more tests for border cases of inspector.substitute()

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
        SpyContext ctx = new SpyContext(SpyDefinition.instance(), "some.Class", "someMethod", "()V", 1);
        SpyRecord rec = new SpyRecord(ctx);
        rec.put("E0", "123"); rec.put("E1", "4567");
        rec.put("R0", "aaa"); rec.put("R1", "bbb");

        assertEquals("123!", ObjectInspector.substitute("${E0}!", rec));
        assertEquals("aaa", ObjectInspector.substitute("${R0}", rec));
        assertEquals("4", ObjectInspector.substitute("${E1.length()}", rec));
    }


    // TODO tests for tabular data


}
