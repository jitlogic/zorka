package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TraceDetailStats;
import com.jitlogic.zorka.core.spy.tuner.TraceSummaryStats;
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

        Tracer.setTuningMode(Tracer.TUNING_DET);
    }


    public TracerTuningUnitTest(String tracerType) {
        configProperties.setProperty("tracer.tuner", "yes");
        configProperties.setProperty("tracer.tuner.qlen", "0");
        configProperties.setProperty("tracer.type", tracerType);
    }


    @Test
    public void detReturnCheckStateUnitTest() throws Exception {

        Tracer.setMinMethodTime(1000);

        h.traceEnter(m[0], 10);
        h.traceReturn(20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));
        assertEquals(1L, getField(h, "tunDrops"));
        assertEquals(0L, getField(h, "tunLCalls"));
        assertEquals(0L, getField(h ,"tunErrors"));

        TraceSummaryStats ts = getField(h, "tunStats");
        assertNotNull(ts);

        TraceDetailStats td = ts.getDetails();
        assertEquals("Should register method call.", 1, td.getRank(m[0]));
    }

    @Test
    public void detReturnDropCheckStateUnitTest() throws Exception {
        Tracer.setMinMethodTime(0);

        h.traceEnter(m[0], 10);
        h.traceReturn(20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));
        assertEquals(0L, getField(h, "tunDrops"));
        assertEquals(0L, getField(h, "tunLCalls"));
        assertEquals(0L, getField(h ,"tunErrors"));
    }

    @Test
    public void detErrorCheckStateUnitTest() throws Exception {
        Tracer.setMinMethodTime(1000);

        h.traceEnter(m[0], 10);
        h.traceError(new NullPointerException(), 20);

        assertEquals(20L, getField(h, "tunLastExchange"));
        assertEquals(1L, getField(h, "tunCalls"));
        assertEquals(1L, getField(h, "tunDrops"));
        assertEquals(0L, getField(h, "tunLCalls"));
        assertEquals(1L, getField(h ,"tunErrors"));
    }

    @Test
    public void detExchangeStateUnitTest() throws Exception {
        Tracer.setTuningDefaultExchInterval(100);

        h.traceEnter(m[0], 5);
        h.traceReturn(10);
        h.traceEnter(m[0], 105);
        h.traceReturn(115);

        assertEquals(1L, getField(tuner, "calls"));
    }

    @Test
    public void detExchangeCycleUnitTest() throws Exception {
        Tracer.setTuningDefaultExchInterval(100);
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
        Tracer.setTuningDefaultExchInterval(100);
        setField(tuner, "interval", 100L);
        setField(tuner, "minTotalCalls", 5L);
        setField(tuner, "minMethodCalls", 0L);
        setField(tuner, "autoMpc", 1);
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
