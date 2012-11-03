package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.agent.testutil.*;
import com.jitlogic.zorka.util.ObjectInspector;

import com.jitlogic.zorka.util.ZorkaLogger;
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
public class ObjectInspectorUnitTest {

    private MBeanServer mbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
    private ObjectInspector inspector;

    @Before
    public void setUp() {
        ZorkaConfig.loadProperties(this.getClass().getResource("/conf").getPath());
        ZorkaLogger.setLogger(new TestLogger());
        inspector  = new ObjectInspector();
    }

    public void tearDown() {
        ZorkaLogger.setLogger(new TestLogger());
        ZorkaConfig.cleanup();
    }


    @Test
    public void testInspectStatic() {

        // Call specifically for methods or fields
        assertEquals(126, inspector.get(TestInspectorClass.class, "count()"));
        assertEquals(123, inspector.get(TestInspectorClass.class, ".count"));
        assertEquals(234, inspector.get(TestInspectorClass.class, ".priv"));

        // Automatic resolve
        assertEquals(125, inspector.get(TestInspectorClass.class, "count"));
    }

    @Test
    public void testInspectArray() {
        String[] array = { "c", "b", "a" };

        assertEquals("b", inspector.get(array, 1));
        assertEquals("c", inspector.get(array, 0));
    }

    @Test
    public void testListArray() {
        String[] array = { "c", "b", "a" };

        assertEquals(Arrays.asList(0,1,2), inspector.list(array));
    }

    @Test
    public void testInspectMap() {
        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 11); map.put("b", 22); map.put("c", 33);

        assertEquals(11, inspector.get(map, "a"));
        assertEquals(3, inspector.get(map, "size()"));
    }

    @Test
    public void testLisMap() {
        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 11); map.put("b", 22); map.put("c", 33);

        assertEquals("sorted map keys", Arrays.asList("a", "b", "c"), inspector.list(map));
    }

    @Test
    public void testInspectList() {
        List<String> list = Arrays.asList("c", "b", "a");

        assertEquals("b", inspector.get(list, 1));
        assertEquals("c", inspector.get(list, 0));
    }

    @Test
    public void testListList() {
        List<String> list = Arrays.asList("c", "b", "a");

        assertEquals(Arrays.asList(0,1,2), inspector.list(list));
    }

    @Test
    public void testInspectJ2eeStats() throws Exception {
        TimeStatistic ts = (TimeStatistic)inspector.get(new TestStats(), "aaa");
        assertNotNull("didn't resolve anything", ts);
        assertEquals("stat name", "aaa", ts.getName());
    }

    @Test
    public void testListJ2eeStats() throws Exception {
        assertEquals(Arrays.asList("aaa", "bbb", "ccc"), inspector.list(new TestStats()));
    }

    @Test
    public void testInspectJmxObj() throws Exception {
        JmxObject jmxObject = mkJmxObject();

        assertEquals(100L, inspector.get(jmxObject, "Nom"));
    }

    @Test
    public void testListJmxObj() throws Exception {
        JmxObject jmxObject = mkJmxObject();

        assertEquals(Arrays.asList("Div", "Nom", "StrMap"), inspector.list(jmxObject));
    }

    private JmxObject mkJmxObject() throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(100);
        bean.setDiv(200);
        ObjectName on = new ObjectName("zorka.test:name=test");
        mbs.registerMBean(bean, on);
        return new JmxObject(on, mbs, null);
    }

    // TODO tests for tabular data
}
