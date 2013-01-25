/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.test.spy;

import com.jitlogic.zorka.common.*;
import com.jitlogic.zorka.agent.spy.*;
import com.jitlogic.zorka.agent.test.spy.support.TestTracer;

import org.junit.Assert;
import org.junit.Test;

public class TraceEnrichmentUnitTest {

    private SymbolRegistry symbols = new SymbolRegistry();


    private ByteBuffer buf = new ByteBuffer();;
    private TestTracer output = new TestTracer();
    private SymbolEnricher enricher = new SymbolEnricher(symbols, output);


    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int s1 = symbols.symbolId("()V");


    @Test
    public void testEnrichTraceEnterCall() {
        enricher.traceEnter(c1, m1, s1, 100L);

        Assert.assertEquals("should receive three symbols and traceEnter", 4, output.size());
        output.check(0, "action", "newSymbol", "symbolId", c1, "symbolName", "some.Class");
        output.check(1, "action", "newSymbol", "symbolId", m1, "symbolName", "someMethod");
    }


    @Test
    public void testEnrichTraceEnterCallTwice() {
        enricher.traceEnter(c1, m1, s1, 100L);
        enricher.traceEnter(c1, m1, s1, 200L);

        Assert.assertEquals("should receive three symbols and traceEnter", 5, output.size());
    }


    @Test
    public void testEnrichTraceError() {
        Exception e = new Exception("oja!");
        enricher.traceError(new WrappedException(e), 100);

        output.check(0, "action", "newSymbol", "symbolId",
                symbols.symbolId("java.lang.Exception"), "symbolName", "java.lang.Exception");

        Assert.assertTrue("Should emit lots of other symbols.", output.size() > 10);

        output.check(output.size()-1, "action", "traceError", "exception",
                new SymbolicException(e, symbols, null), "tstamp", 100L);
    }
}
