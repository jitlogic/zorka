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

package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.perfmon.HiccupMeter;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

public class HiccupMeterUnitTest extends ZorkaFixture {

    @Test
    public void testSimpleCpuHiccupRun() throws Exception {
        HiccupMeter meter = perfmon.cpuHiccup("test", "zorka:name=TestHiccup", "CPU");
        meter.cycle(100);
        assertEquals(0, meter.getStats().getCalls());
        meter.cycle(500);
        assertEquals(1, meter.getStats().getCalls());
    }


    @Test
    public void testSimpleCpuHiccupRunAndCheckFirstResult() throws Exception {
        HiccupMeter meter = perfmon.cpuHiccup("test", "zorka:name=TestHiccup", "CPU", 1, 1);
        meter.cycle(10);
        meter.cycle(1000100);
        assertEquals(90, meter.getStats().getMaxTimeNs());
    }


    @Test
    public void testSimpleCpuHiccupRunAndCheckResultInUs() throws Exception {
        HiccupMeter meter = perfmon.cpuHiccup("test", "zorka:name=TestHiccup", "CPU", 1, 1);
        meter.cycle(10);
        meter.cycle(1001010);
        assertEquals(1, meter.getStats().getMaxTimeUsCLR());
    }

    @Test
    public void testTwoHiccupRuns() throws Exception {
        HiccupMeter meter = perfmon.cpuHiccup("test", "zorka:name=TestHiccup", "CPU", 1, 1);
        meter.cycle(10);
        meter.cycle(1000100);
        assertEquals(90, meter.getStats().getMaxTimeNsCLR());
        meter.cycle(2001000);
        assertEquals(900, meter.getStats().getMaxTimeNsCLR());
    }
}
