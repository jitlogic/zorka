package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TraceTuningStats;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.jitlogic.zorka.common.util.ObjectInspector.setField;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.getField;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TracerTuningUnitTest extends ZorkaFixture {

    @Parameterized.Parameters(name="tracer={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"local"}, {"streaming"}});
    }


    private int[] m ;
    private TraceHandler h;
    private TracerTuner tuner;


    @Before
    public void setUpTuner() {
        this.tuner = agentInstance.getTracerTuner();
        assertNotNull(tuner);

        tracer.output(new ZorkaSubmitter<SymbolicRecord>() {
            @Override
            public boolean submit(SymbolicRecord item) {
                return true;
            }
        });

        this.h = agentInstance.getTracer().getHandler();

        m = new int[2];

        m[0] = symbols.methodId(symbols.symbolId("some.Class"), symbols.symbolId("someMethod"), symbols.symbolId("()V"));
        m[1] = symbols.methodId(symbols.symbolId("other.Class"), symbols.symbolId("someMethod"), symbols.symbolId("()V"));

        TraceHandler.setTuningEnabled(true);
        TraceHandler.setTuningExchangeMinCalls(0);
    }


    public TracerTuningUnitTest(String tracerType) {
        configProperties.setProperty("tracer.tuner", "yes");
        configProperties.setProperty("tracer.tuner.qlen", "0");
        configProperties.setProperty("tracer.type", tracerType);
    }


    @Test
    public void detReturnCheckStateUnitTest() throws Exception {

        TraceHandler.setMinMethodTime(1000);

        h.traceEnter(m[0], 10);
        h.traceReturn(20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));

        TraceTuningStats ts = getField(h, "tunStats");
        assertNotNull(ts);

        assertEquals("Should register method call.", 1, ts.getRank(m[0]));
    }

    @Test
    public void detReturnDropCheckStateUnitTest() throws Exception {
        TraceHandler.setMinMethodTime(0);

        h.traceEnter(m[0], 10);
        h.traceReturn(20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));
    }

    @Test
    public void detErrorCheckStateUnitTest() throws Exception {
        TraceHandler.setMinMethodTime(1000);

        h.traceEnter(m[0], 10);
        h.traceError(new NullPointerException(), 20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));
    }

    @Test
    public void detExchangeStateUnitTest() throws Exception {
        TraceHandler.setTuningDefaultExchInterval(100);

        h.traceEnter(m[0], 5);
        h.traceReturn(10);
        h.traceEnter(m[0], 105);
        h.traceReturn(115);

        assertEquals(1L, getField(tuner, "calls"));
    }

    @Test
    public void detExchangeCycleUnitTest() throws Exception {
        TraceHandler.setTuningDefaultExchInterval(100);
        setField(tuner, "interval", 100L);

        h.traceEnter(m[0], 5);
        h.traceReturn(10);
        h.traceEnter(m[0], 105);
        h.traceReturn(115);
        h.traceEnter(m[0], 115);
        h.traceReturn(220);

        assertEquals(1, tuner.getRankList().size());
    }

    @Test
    public void detExchangeCycleRetransformUnitTest() throws Exception {
        TraceHandler.setTuningDefaultExchInterval(100);
        setField(tuner, "interval", 100L);
        setField(tuner, "minTotalCalls", 5L);
        setField(tuner, "minMethodRank", 0L);
        setField(tuner, "maxItems", 1);
        setField(tuner, "auto", true);

        h.traceEnter(m[0], 5);
        h.traceReturn(10);

        for (int i = 0; i < 20; i++) {
            h.traceEnter(m[1], 105);
            h.traceReturn(115);
        }

        h.traceEnter(m[0], 115);
        h.traceReturn(220);

        assertEquals(21, tuner.getLastCalls());
        assertEquals(1, spyRetransformer.getClassNames().size());

    }

    // TODO debugging messages in TraceTuner class
}
