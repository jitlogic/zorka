package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.perfmon.ThreadMonitorItem;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThreadMonitorUnitTest extends ZorkaFixture {

    @Test
    public void testThreadMonitorItemSubmitConstAvg() {
        ThreadMonitorItem tmi = new ThreadMonitorItem(1, "", 4);

        assertEquals(0.0, tmi.avg(500), 0.01);

        tmi.submit(100, 100);
        assertEquals(0.0, tmi.avg(500), 0.01);

        for (int i = 2; i < 10; i++) {
            tmi.submit(i*100, i*100);
            assertEquals("i="+i, 100.0, tmi.avg(500), 0.01);
        }
    }

    @Test
    public void testThreadMonitorItemSubmitVaryingAvg() {
        ThreadMonitorItem tmi = new ThreadMonitorItem(1, "", 4);

        assertEquals(0.0, tmi.avg(500), 0.01);

        tmi.submit(100, 100);
        assertEquals(0.0, tmi.avg(500), 0.01);

        tmi.submit(200, 200);
        assertEquals(100.0, tmi.avg(500), 0.01);

        tmi.submit(300, 200);
        assertEquals(50.0, tmi.avg(500), 0.01);
    }


}
