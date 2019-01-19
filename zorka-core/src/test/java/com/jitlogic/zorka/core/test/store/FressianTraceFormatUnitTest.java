/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.fressian.FressianReader;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.common.tracedata.FressianTraceFormat.READ_LOOKUP;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FressianTraceFormatUnitTest extends ZorkaFixture {

    private ByteArrayOutputStream output = new ByteArrayOutputStream();
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


    @Before
    public void setUp() {
        writer = new FressianTraceWriter(symbols, metrics);
        writer.setOutput(mkf(output));
    }

    @Test
    public void testReadWriteSimpleSymbol() throws Exception {
        symbols.put(10, "oja!");
        writer.checkSymbol(10, null);

        assertEquals(new Symbol(10, "oja!"), reader().readObject());
    }


    @Test @Ignore("deprecated")
    public void testReadWriteMetricTemplate() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.WINDOWED_RATE, "", "test", "Test Metric", "m/s", "nomNom", "divDiv")
                        .multiply(2.0));

        writer.checkTemplate(mt.getId());

        MetricTemplate mt2 = (MetricTemplate) reader().readObject();

        assertEquals(mt.getId(), mt2.getId());
        assertEquals(mt.getDescription(), mt2.getDescription());
        assertEquals(mt.getUnits(), mt2.getUnits());
        assertEquals(mt.getNomField(), mt2.getNomField());
        assertEquals(mt.getDivField(), mt2.getDivField());
        assertEquals(2.0, mt2.getMultiplier(), 0.01);
        assertEquals(MetricTemplate.WINDOWED_RATE, mt2.getType());
    }


    @Test @Ignore("deprecated")
    public void testReadWriteMetricTemplateWithAttrs() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.WINDOWED_RATE, "", "test", "Test Metric", "m/s", "nomNom", "divDiv")
                        .dynamicAttrs("a", "b", "c", "d"));

        writer.checkTemplate(mt.getId());

        MetricTemplate mt2 = (MetricTemplate) reader().readObject();

        assertEquals(ZorkaUtil.set("a", "b", "c", "d"), mt2.getDynamicAttrs());
    }


    @Test @Ignore("deprecated")
    public void testReadWriteMetric() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.RAW_DATA, "", "test", "Test Metric", "m/s", "nomNom", "divDiv"));

        Metric m = metrics.getMetric(
                new RawDataMetric(0, mt.getId(), "", "test", "Test", ZorkaUtil.<String, Object>map("a", 1, "b", 2)));

        m.setTemplate(mt);
        m.setTemplateId(mt.getId());

        writer.checkMetric(m.getId());

        FressianReader reader = reader();

        MetricTemplate mt2 = (MetricTemplate) reader.readObject();
        Metric m2 = (Metric) reader.readObject();

        assertNotNull(m2);

        assertEquals(m.getId(), m2.getId());
        assertEquals(m.getDescription(), m2.getDescription());
        assertEquals(ZorkaUtil.<String, Object>constMap("a", 1L, "b", 2L), m2.getAttrs());
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
            assertEquals(sid(s.getName()), s.getId());
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertEquals(TraceRecord.EXCEPTION_PASS, tr2.getFlags());
        assertEquals(tr.getCalls(), tr2.getCalls());
        assertEquals(tr.getTime(), tr2.getTime());
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
            assertEquals(sid(s.getName()), s.getId());
            obj = reader.readObject();
            ids.add(s.getId());
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertEquals(TraceRecord.TRACE_BEGIN, tr2.getFlags());

        TraceMarker tm2 = tr2.getMarker();

        assertNotNull(tm2);
        assertEquals(TraceMarker.OVERFLOW_FLAG, tm2.getFlags());
        assertEquals(100L, tm2.getClock());
        assertEquals(sid("TRACE"), tm2.getTraceId());
        assertEquals(symbols.size(), ids.size());
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
            assertEquals(sid(s.getName()), s.getId());
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;
        assertEquals(1, tr2.numChildren());
        assertEquals(50L, tr2.getChild(0).getTime());
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
            assertEquals(sid(s.getName()), s.getId());
            obj = reader.readObject();
        }

        TraceRecord tr2 = (TraceRecord) obj;

        SymbolicException se = (SymbolicException) tr2.getException();
        assertEquals("oja!", se.getMessage());
        assertEquals(sid("java.lang.Exception"), se.getClassId());
        assertNotNull(se.getStackTrace());

        SymbolicStackElement sse = se.getStackTrace()[0];
        assertEquals(sid(this.getClass().getName()), sse.getClassId());
    }


    private PerfSample ps(Metric m, long clock, Number val) {
        PerfSample ps = new PerfSample(m.getId(), val);
        ps.setClock(clock);
        return ps;
    }


    @Test @Ignore("deprecated")
    public void testReadWritePerfDataRecord() throws Exception {
        MetricTemplate mt = metrics.getTemplate(
                new MetricTemplate(0, MetricTemplate.RAW_DATA, "", "test", "Test Metric", "m/s", "nomNom", "divDiv"));

        Metric m = metrics.getMetric(
                new RawDataMetric(0, mt.getId(), "", "test", "Test", ZorkaUtil.<String, Object>map("a", 1, "b", 2)));

        m.setTemplate(mt);
        m.setTemplateId(mt.getId());

        PerfRecord pr = new PerfRecord(100L, sid("PERF"), Arrays.asList(ps(m, 100L, 100L), ps(m, 200L, 200L)));

        writer.write(pr);

        FressianReader reader = reader();

        Object obj = reader.readObject();

        while (obj != null && !(obj instanceof PerfRecord)) {
            if (obj instanceof Symbol) {
                Symbol s = (Symbol) obj;
                assertEquals(sid(s.getName()), s.getId());
            }
            obj = reader.readObject();
        }

        PerfRecord pr2 = (PerfRecord) obj;
        assertNotNull(pr2);
        assertEquals(100L, pr2.getClock());
        assertEquals(sid("PERF"), pr2.getScannerId());
        assertEquals(Arrays.asList(ps(m, 100L, 100L), ps(m, 200L, 200L)), pr2.getSamples());
    }

}
