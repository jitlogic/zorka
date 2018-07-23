package com.jitlogic.zorka.core.test.scripting;

import com.jitlogic.zorka.common.tracedata.TracerOutput;
import com.jitlogic.zorka.common.zico.ZicoTraceOutput;
import com.jitlogic.zorka.core.AgentInstance;

import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class TracerBshUnitTest extends BshTestFixture {

    @Test
    public void testTraceBshDefaultConf() {
        AgentInstance inst = checkLoadScript("tracer.bsh");

        assertFalse(inst.getConfig().boolCfg("tracer", null));
        assertEquals(0, inst.getTracer().getOutputs().size());
    }

    @Test
    public void testTraceBshTracerEnabled() {
        AgentInstance inst = checkLoadScript("tracer.bsh", "tracer", "yes");

        assertTrue(inst.getConfig().boolCfg("tracer", null));

        Tracer tracer = inst.getTracer();

        assertEquals(1, tracer.getOutputs().size());
        assertTrue(tracer.getOutputs().get(0) instanceof ZicoTraceOutput);

        SpyMatcherSet ms = tracer.getMatcherSet();
        assertFalse(ms.classMatch("java.lang.String"));
        assertFalse(ms.classMatch("$Proxy_SomeClassOrIface"));
        assertFalse(ms.classMatch("com.jitlogic.zorka.core.spy.SpyTransformer"));
        assertTrue(ms.classMatch("com.myapp.MyClass"));
    }

    @Test
    public void testDtraceFreshInput() {
        AgentInstance inst = checkLoadScript("tracer.bsh");

        SpyProcessor p = (SpyProcessor)inst.getZorkaAgent().eval("dtrace.trace_in()");
        assertNotNull(p);

        Map<String,Object> r = p.process(rec());
        assertNotNull(r.get("DTRACE_UUID"));
        assertEquals("", r.get("DTRACE_IN"));

        ThreadLocal uuidLocal = (ThreadLocal)(inst.getZorkaAgent().eval("dtrace.uuidLocal"));
        assertEquals(r.get("DTRACE_UUID"), uuidLocal.get());

        ThreadLocal tidLocal = (ThreadLocal)(inst.getZorkaAgent().eval("dtrace.tidLocal"));
        assertEquals(r.get("DTRACE_IN"), tidLocal.get());
    }

    @Test
    public void testDtracePopulatedInput() {
        AgentInstance inst = checkLoadScript("tracer.bsh");

        SpyProcessor p = (SpyProcessor)inst.getZorkaAgent().eval("dtrace.trace_in()");
        assertNotNull(p);

        String uuid = UUID.randomUUID().toString(), tid = "/123";

        Map<String,Object> r = p.process(rec("DTRACE_UUID", uuid, "DTRACE_IN", tid));
        assertEquals(uuid, r.get("DTRACE_UUID"));
        assertEquals(tid, r.get("DTRACE_IN"));

        ThreadLocal uuidLocal = (ThreadLocal)(inst.getZorkaAgent().eval("dtrace.uuidLocal"));
        assertEquals(r.get("DTRACE_UUID"), uuidLocal.get());

        ThreadLocal tidLocal = (ThreadLocal)(inst.getZorkaAgent().eval("dtrace.tidLocal"));
        assertEquals(r.get("DTRACE_IN"), tidLocal.get());
    }
}
