package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.TestInspectorClass;
import com.jitlogic.zorka.util.ObjectInspector;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspectionUnitTest {

    private ObjectInspector inspector = new ObjectInspector();

    @Test
    public void testInspectStaticMethodInClass() {
        assertEquals(123, inspector.get(TestInspectorClass.class, "count()"));
    }

}
