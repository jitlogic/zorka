/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.test.tracer;

import com.jitlogic.zorka.test.spy.support.TestTracer;
import com.jitlogic.zorka.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.test.support.BytecodeInstrumentationFixture.*;
import static com.jitlogic.zorka.test.support.TestUtil.*;

public class TracerIntegTest extends ZorkaFixture {

    private TestTracer output = new TestTracer();

    @Test
    public void testSimpleTrace() throws Exception {
        spy.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
            spy.instance().onEnter(spy.traceBegin("TEST"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        spyInstance.getTracer().setMethodTime(0); // Catch everything
        spy.add(output);

        Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return traceBegin, trace", 4, output.size());
        output.check(0, "action", "traceBegin");
        output.check(1, "action", "traceEnter");
        output.check(2, "action", "traceStats", "calls", 1L, "errors", 0L);
        output.check(3, "action", "traceReturn");
    }

}
