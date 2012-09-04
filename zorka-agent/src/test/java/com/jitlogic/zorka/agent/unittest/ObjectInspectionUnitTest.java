package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.TestInspectorClass;
import com.jitlogic.zorka.util.ObjectInspector;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspectionUnitTest {

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


}
