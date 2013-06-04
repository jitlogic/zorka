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

package com.jitlogic.zorka.core.store;

import com.jitlogic.zorka.core.perfmon.Metric;
import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.perfmon.PerfDataEventHandler;
import com.jitlogic.zorka.core.spy.TraceMarker;
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.util.PerfSample;

import java.util.ArrayList;
import java.util.List;

public class TraceReader implements PerfDataEventHandler {

    private List<TraceRecord> results = new ArrayList<TraceRecord>();
    private TraceRecord root = new TraceRecord(null), cur;
    private SymbolRegistry symbols;

    public TraceReader() {

    }

    public TraceReader(SymbolRegistry symbols) {
        this.symbols = symbols;
    }

    public List<TraceRecord> getResults() {
        return results;
    }

    public TraceRecord getRoot() {
        return root;
    }

    @Override
    public void traceStats(long calls, long errors, int flags) {
        if (cur != null) {
            cur.setCalls(calls);
            cur.setErrors(errors);
            cur.setFlags(flags);
        }
    }

    @Override
    public void newSymbol(int symbolId, String symbolText) {
        if (symbols != null) {
            symbols.put(symbolId, symbolText);
        }
    }

    @Override public void perfData(long clock, int scannerId, List<PerfSample> samples) { }
    @Override public void newMetricTemplate(MetricTemplate template) { }
    @Override public void newMetric(Metric metric) { }

    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        TraceRecord rec = cur != null ? cur : root;
        TraceMarker m = new TraceMarker(rec, traceId, clock);
        m.setFlags(flags);
        rec.setMarker(m);
    }

    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        cur = (cur == null) ? root : new TraceRecord(cur);

        cur.setClassId(classId);
        cur.setMethodId(methodId);
        cur.setSignatureId(signatureId);
        cur.setTime(tstamp);
    }

    @Override
    public void traceReturn(long tstamp) {
        pop(tstamp);
    }

    @Override
    public void traceError(Object exception, long tstamp) {
        cur.setException(exception);
        pop(tstamp);
    }

    private void pop(long tstamp) {
        cur.setTime(tstamp);
        cur = cur.getParent();

        if (cur == null) {
            results.add(root);
            root = new TraceRecord(null);
        }
    }

    @Override
    public void newAttr(int attrId, Object attrVal) {
        cur.setAttr(attrId, attrVal);
    }

    @Override
    public void disable() {

    }

    @Override
    public void enable() {

    }
}
