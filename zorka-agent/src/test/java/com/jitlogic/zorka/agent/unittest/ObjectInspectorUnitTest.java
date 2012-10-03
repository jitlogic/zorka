package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.TestInspectorClass;
import com.jitlogic.zorka.agent.testutil.TestStats;
import com.jitlogic.zorka.util.ObjectInspector;

import org.junit.Test;

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

    private ObjectInspector inspector = new ObjectInspector();


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
    public void testInspectMap() {


        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 11); map.put("b", 22); map.put("c", 33);

        assertEquals(11, inspector.get(map, "a"));
        assertEquals(3, inspector.get(map, "size()"));
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
}
