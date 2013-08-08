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
package com.jitlogic.zorka.common.test.support;


import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.util.Random;

public class TestTraceGenerator {

    private final static String[] traceMarkers = { "MY_TRACE" };
    private final static String[] classNames = { "my/pkg/SomeClass", "my/pkg/OtherClass" };
    private final static String[] methodNames = { "someMethod", "otherMethod" };
    private final static String[] methodSignatures = { "()V" };

    private SymbolRegistry symbols;
    private MetricsRegistry metrics;


    private Random random = new Random();

    public TestTraceGenerator() {
        this(new SymbolRegistry(), new MetricsRegistry());
    }

    public TestTraceGenerator(SymbolRegistry symbols, MetricsRegistry metrics) {
        this.symbols = symbols;
        this.metrics = metrics;
    }

    public SymbolRegistry getSymbols() {
        return symbols;
    }

    public MetricsRegistry getMetrics() {
        return metrics;
    }

    public TraceRecord generate() {
        TraceRecord tr = new TraceRecord(null);

        tr.setClassId(rsid(classNames));
        tr.setMethodId(rsid(methodNames));
        tr.setSignatureId(rsid(methodSignatures));
        tr.setTime(rmtime());
        tr.setFlags(TraceRecord.TRACE_BEGIN);

        TraceMarker tm = new TraceMarker(tr, rsid(traceMarkers), 100);
        tr.setMarker(tm);

        return tr;
    }


    private long rmtime() {
        return 50000;
    }


    private int rsid(String...inputs) {
        return symbols.symbolId(inputs[random.nextInt(inputs.length)]);
    }
}
