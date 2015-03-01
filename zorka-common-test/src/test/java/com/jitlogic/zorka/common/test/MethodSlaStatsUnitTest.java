/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.stats.MethodCallStatistic;

import com.jitlogic.zorka.common.util.ObjectInspector;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodSlaStatsUnitTest {

    private static final long S = 1000000000L;

    @Test
    public void testSubmitSomeDataAndCheckSla() {
        MethodCallStatistic m = new MethodCallStatistic("test");

        m.logCall(5 * S);
        assertEquals(100.0, m.getSla().getStatistic("2000").getSla(), 0.1);

        m.logCall(5 * S);
        assertEquals(0.0, m.getSla().getStatistic("2000").getSla(), 0.1);
        assertEquals(100.0, m.getSla().getStatistic("4000").getSla(), 0.1);

        m.logCall(1 * S);
        assertEquals(100.0, m.getSla().getStatistic("4000").getSla(), 0.1);

        assertEquals(50.0, m.getSla().getStatistic("2000").getSlaCLR(), 0.1);
        assertEquals(100.0, m.getSla().getStatistic("2000").getSla(), 0.1);
    }

    @Test
    public void testCheckIfSlaIsAccessibleViaObjectInspector() {
        MethodCallStatistic m = new MethodCallStatistic("test");

        assertEquals(100.0, (Double)ObjectInspector.get(m, "sla", "4000", "sla"), 0.1);

        m.logCall(S); m.logError(S);

        assertEquals(50.0, (Double)ObjectInspector.get(m, "sla", "4000", "sla"), 0.1);
    }

    @Test
    public void testIfErrorsAlsoMatch() {
        MethodCallStatistic m = new MethodCallStatistic("test");
        assertEquals(100.0, m.getSla().getStatistic("2000").getSla(), 0.1);
        m.logError(S);
        assertEquals(0.0, m.getSla().getStatistic("2000").getSla(), 0.1);
    }

}
