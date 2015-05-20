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
package com.jitlogic.zorka.core.test.store;


import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.fressian.FressianReader;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

import static com.jitlogic.zorka.common.tracedata.FressianTraceFormat.READ_LOOKUP;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FressianTraceFormatUnitTest {

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    private SymbolRegistry symbols = new SymbolRegistry();
    private MetricsRegistry metrics = new MetricsRegistry();
    private FressianTraceWriter writer;

    private TraceStreamOutput mkf(final OutputStream output) {
        return new TraceStreamOutput() {
            @Override
            public OutputStream getOutputStream() {
                return output;
            }
        };
    }

    private FressianReader reader() {
        return new FressianReader(new ByteArrayInputStream(output.toByteArray()), READ_LOOKUP);
    }

    private void save(String path) throws Exception {
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(output.toByteArray());
        fos.close();
    }

    private int sid(String symbol) {
        return symbols.symbolId(symbol);
    }


    private Symbol sym(String name) {
        return new Symbol(symbols.symbolId(name), name);
    }


    private Symbol sym(int id, String name) {
        return new Symbol(id, name);
    }


    @Before
    public void setUp() {
        writer = new FressianTraceWriter(symbols, metrics);
        writer.setOutput(mkf(output));
    }

    @Test
    public void testReadWriteSimpleSymbol() throws Exception {
        symbols.put(10, "oja!");
        writer.checkSymbol(10, null);

        assertThat(reader().readObject()).isEqualTo(new Symbol(10, "oja!"));
    }


    @Test
    public void testReadWriteMetricTemplate() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.WINDOWED_RATE, "test", "Test Metric", "m/s", "nomNom", "divDiv")
                        .multiply(2.0));

        writer.checkTemplate(mt.getId());

        MetricTemplate mt2 = (MetricTemplate) reader().readObject();

        assertThat(mt2.getId()).isEqualTo(mt.getId());
        assertThat(mt2.getDescription()).isEqualTo(mt.getDescription());
        assertThat(mt2.getUnits()).isEqualTo(mt.getUnits());
        assertThat(mt2.getNomField()).isEqualTo(mt.getNomField());
        assertThat(mt2.getDivField()).isEqualTo(mt.getDivField());
        assertThat(mt2.getMultiplier()).isEqualTo(2.0);
        assertThat(mt2.getType()).isEqualTo(MetricTemplate.WINDOWED_RATE);
    }


    @Test
    public void testReadWriteMetricTemplateWithAttrs() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.WINDOWED_RATE, "test", "Test Metric", "m/s", "nomNom", "divDiv")
                        .dynamicAttrs("a", "b", "c", "d"));

        writer.checkTemplate(mt.getId());

        MetricTemplate mt2 = (MetricTemplate) reader().readObject();

        assertThat(mt2.getDynamicAttrs()).isEqualTo(ZorkaUtil.<String>set("a", "b", "c", "d"));
    }


    @Test
    public void testReadWriteMetric() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.RAW_DATA, "test", "Test Metric", "m/s", "nomNom", "divDiv"));

        Metric m = metrics.getMetric(
                new RawDataMetric(0, mt.getId(), "test", "Test", ZorkaUtil.<String, Object>map("a", 1, "b", 2)));

        m.setTemplate(mt);
        m.setTemplateId(mt.getId());

        writer.checkMetric(m.getId());

        FressianReader reader = reader();

        MetricTemplate mt2 = (MetricTemplate) reader.readObject();
        Metric m2 = (Metric) reader.readObject();

        assertThat(m2).isInstanceOfAny(RawDataMetric.class);

        assertThat(m2.getId()).isEqualTo(m.getId());
        assertThat(m2.getDescription()).isEqualTo(m.getDescription());
        assertThat(m2.getAttrs()).isEqualTo(ZorkaUtil.<String, Object>constMap("a", 1L, "b", 2L));
    }

    public TraceRecord tr(String className, String methodName, String methodSignature,
                          long calls, long errors, int flags, long time,
                          TraceRecord... children) {
        TraceRecord tr = new TraceRecord(null);
        tr.setClassId(sid(className));
        tr.setMethodId(sid(methodName));
        tr.setSignatureId(sid(methodSignature));
        tr.setCalls(calls);
        tr.setErrors(errors);
        tr.setFlags(flags);
        tr.setTime(time);

        for (TraceRecord child : children) {
            child.setParent(tr);
            tr.addChild(child);
        }

        return tr;
    }

    @Test
    public void testReadWriteTraceRecordWithoutMarker() throws Exception {
        TraceRecord tr = tr("some.Class", "someMethod", "()V", 1, 0, TraceRecord.EXCEPTION_PASS, 100);
        tr.setAttr(sid("ATTR1"), 10);
        tr.setAttr(sid("ATTR2"), "bork");
        tr.setAttr(sid("ATTR3"), 1.23);

        writer.write(tr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        while (obj instanceof Symbol) {
            Symbol s = (Symbol) obj;
            assertThat(s.getId()).isEqualTo(sid(s.getName()));
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertThat(tr2.getFlags()).isEqualTo(TraceRecord.EXCEPTION_PASS);
        assertThat(tr2.getCalls()).isEqualTo(tr.getCalls());
        assertThat(tr2.getTime()).isEqualTo(tr.getTime());
    }


    @Test
    public void testReadWriteTraceRecordWithMarker() throws Exception {
        TraceRecord tr = tr("some.Class", "someMethod", "()V", 1, 0, TraceRecord.TRACE_BEGIN, 100);
        TraceMarker tm = new TraceMarker(tr, sid("TRACE"), 100L);
        tm.setFlags(TraceMarker.OVERFLOW_FLAG);
        tr.setMarker(tm);

        writer.write(tr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        Set<Integer> ids = new HashSet<Integer>();

        while (obj instanceof Symbol) {
            Symbol s = (Symbol) obj;
            assertThat(s.getId()).isEqualTo(sid(s.getName()));
            obj = reader.readObject();
            ids.add(s.getId());
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertThat(tr2.getFlags()).isEqualTo(TraceRecord.TRACE_BEGIN);

        TraceMarker tm2 = tr2.getMarker();

        assertThat(tm2).isNotNull();
        assertThat(tm2.getFlags()).isEqualTo(TraceMarker.OVERFLOW_FLAG);
        assertThat(tm2.getClock()).isEqualTo(100L);
        assertThat(tm2.getTraceId()).isEqualTo(sid("TRACE"));

        assertThat(ids.size()).isEqualTo(symbols.size());

    }


    @Test
    public void testReadWriteRecursiveTrace() throws Exception {
        TraceRecord tr =
                tr("some.Class", "someMethod", "()V", 2, 0, 0, 100,
                        tr("other.Class", "otherMethod", "()V", 1, 0, 0, 50));

        writer.write(tr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        while (obj instanceof Symbol) {
            Symbol s = (Symbol) obj;
            assertThat(s.getId()).isEqualTo(sid(s.getName()));
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertThat(tr2.numChildren()).isEqualTo(1);
        assertThat(tr2.getChild(0).getTime()).isEqualTo(50L);
    }


    @Test
    public void testReadWriteTraceWithExceptionObject() throws Exception {
        TraceRecord tr =
                tr("some.Class", "someMethod", "()V", 2, 0, 0, 100,
                        tr("other.Class", "otherMethod", "()V", 1, 0, 0, 50));

        tr.setException(new Exception("oja!"));
        tr.fixup(symbols);

        writer.write(tr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        while (obj instanceof Symbol) {
            Symbol s = (Symbol) obj;
            assertThat(s.getId()).isEqualTo(sid(s.getName()));
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;

        SymbolicException se = (SymbolicException) tr2.getException();
        assertThat(se.getMessage()).isEqualTo("oja!");
        assertThat(se.getClassId()).isEqualTo(sid("java.lang.Exception"));
        assertThat(se.getStackTrace()).isNotNull();

        SymbolicStackElement sse = se.getStackTrace()[0];
        assertThat(sse.getClassId()).isEqualTo(sid(this.getClass().getName()));
    }


    private PerfSample ps(Metric m, long clock, Number val) {
        PerfSample ps = new PerfSample(m.getId(), val);
        ps.setClock(clock);
        return ps;
    }


    @Test
    public void testReadWritePerfDataRecord() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.RAW_DATA, "test", "Test Metric", "m/s", "nomNom", "divDiv"));

        Metric m = metrics.getMetric(
                new RawDataMetric(0, mt.getId(), "test", "Test", ZorkaUtil.<String, Object>map("a", 1, "b", 2)));

        m.setTemplate(mt);
        m.setTemplateId(mt.getId());

        PerfRecord pr = new PerfRecord(100L, sid("PERF"), Arrays.asList(ps(m, 100L, 100L), ps(m, 200L, 200L)));

        writer.write(pr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        while (obj != null && !(obj instanceof PerfRecord)) {
            if (obj instanceof Symbol) {
                Symbol s = (Symbol) obj;
                assertThat(s.getId()).isEqualTo(sid(s.getName()));
            }
            obj = reader.readObject();
        }

        PerfRecord pr2 = (PerfRecord) obj;
        assertThat(pr2).isNotNull();
        assertThat(pr2.getClock()).isEqualTo(100L);
        assertThat(pr2.getScannerId()).isEqualTo(sid("PERF"));
        assertThat(pr2.getSamples()).isEqualTo(Arrays.asList(ps(m, 100L, 100L), ps(m, 200L, 200L)));
    }

}
