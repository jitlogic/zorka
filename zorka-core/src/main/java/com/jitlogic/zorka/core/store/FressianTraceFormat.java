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


import com.jitlogic.zorka.core.util.ZorkaUtil;
import org.fressian.Reader;
import org.fressian.Writer;
import org.fressian.handlers.ILookup;
import org.fressian.handlers.ReadHandler;
import org.fressian.handlers.WriteHandler;
import org.fressian.impl.ChainedLookup;
import org.fressian.impl.Handlers;
import org.fressian.impl.InheritanceLookup;
import org.fressian.impl.MapLookup;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FressianTraceFormat {

    public static final String SYMBOL_TAG     = "com.jitlogic.zorka.Symbol_v1";
    public static final String METRIC_TAG     = "com.jitlogic.zorka.Metric_v1";
    public static final String TEMPLATE_TAG   = "com.jitlogic.zorka.MetricTemplate_v1";
    public static final String RECORD_TAG     = "com.jitlogic.zorka.TraceRecord_v1";
    public static final String MARKER_TAG     = "com.jitlogic.zorka.TraceMarker_v1";
    public static final String EXCEPTION_TAG  = "com.jitlogic.zorka.SymbolicException_v1";
    public static final String STACKEL_TAG    = "com.jitlogic.zorka.SymbolicStackElement_v1";
    public static final String PERFRECORD_TAG = "com.jitlogic.zorka.PerfRecord_v1";
    public static final String PERFSAMPLE_TAG = "com.jitlogic.zorka.PerfSample_v1";


    private static final WriteHandler NULL_WRITE_HANDLER = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            w.writeNull();
        }
    };


    private static final WriteHandler SYMBOL_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            Symbol s = (Symbol)instance;

            w.writeTag(SYMBOL_TAG, 2);
            w.writeInt(s.getId());
            w.writeObject(s.getName());
        }
    };


    private static final ReadHandler SYMBOL_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new Symbol((int)r.readInt(), (String)r.readObject());
        }
    };


    private static final WriteHandler RECORD_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            TraceRecord tr = (TraceRecord)instance;
            Object e = tr.getException();

            w.writeTag(RECORD_TAG, 11);

            w.writeInt(tr.getClassId());
            w.writeInt(tr.getMethodId());
            w.writeInt(tr.getSignatureId());
            w.writeInt(tr.getFlags());
            w.writeInt(tr.getTime());
            w.writeInt(tr.getCalls());
            w.writeInt(tr.getErrors());
            w.writeObject(tr.hasFlag(TraceRecord.TRACE_BEGIN) ? tr.getMarker() : null);
            w.writeObject(e instanceof SymbolicException ? e : null);
            w.writeObject(tr.getAttrs());  // TODO make sure unknown data types won't be transmitted
            w.writeList(tr.getChildren());
        }
    };


    public static interface TraceRecordBuilder {
        TraceRecord get();
    }


    public static TraceRecordBuilder TRACE_RECORD_BUILDER = new TraceRecordBuilder() {
        @Override
        public TraceRecord get() {
            return new TraceRecord(null);
        }
    };


    private static final ReadHandler RECORD_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            TraceRecord tr = TRACE_RECORD_BUILDER.get();
            tr.setClassId((int)r.readInt());
            tr.setMethodId((int)r.readInt());
            tr.setSignatureId((int)r.readInt());
            tr.setFlags((int)r.readInt());
            tr.setTime(r.readInt());
            tr.setCalls(r.readInt());
            tr.setErrors(r.readInt());
            tr.setMarker((TraceMarker)r.readObject());
            tr.setException(r.readObject());
            tr.setAttrs((Map<Integer,Object>)r.readObject());

            List<TraceRecord> children = (List<TraceRecord>)r.readObject();

            if (children != null) {
                for (TraceRecord c : children) {
                    c.setParent(tr);
                }
                tr.setChildren(children);
            }

            return tr;
        }
    };


    private static final WriteHandler METRIC_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            Metric m = (Metric)instance;

            w.writeTag(METRIC_TAG, 4);

            w.writeInt(m.getId());
            w.writeInt(m.getTemplate().getType());
            w.writeInt(m.getTemplateId());
            w.writeString(m.getName());
            w.writeObject(m.getAttrs());
        }
    };


    private static final ReadHandler METRIC_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            int id = (int)r.readInt();
            int type = (int)r.readInt();
            int templateId = (int)r.readInt();
            String name = (String)r.readObject();

            Map<String,Object> attrs = (Map<String,Object>)r.readObject();

            switch (type) {
                case MetricTemplate.RAW_DATA:
                    return new RawDataMetric(id, templateId, name, attrs);
                case MetricTemplate.RAW_DELTA:
                    return new RawDeltaMetric(id, templateId, name, attrs);
                case MetricTemplate.TIMED_DELTA:
                    return new TimedDeltaMetric(id, templateId, name, attrs);
                case MetricTemplate.UTILIZATION:
                    return new UtilizationMetric(id, templateId, name, attrs);
                case MetricTemplate.WINDOWED_RATE:
                    return new WindowedRateMetric(id, templateId, name, attrs);
            }

            return null;
        }
    };


    private static final WriteHandler TEMPLATE_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            MetricTemplate t = (MetricTemplate)instance;

            w.writeTag(TEMPLATE_TAG, 8);

            w.writeInt(t.getId());
            w.writeInt(t.getType());
            w.writeString(t.getName());
            w.writeString(t.getUnits());
            w.writeString(t.getNomField());
            w.writeString(t.getDivField());
            w.writeDouble(t.getMultiplier());
            w.writeObject(t.getDynamicAttrs());
        }
    };


    private static final ReadHandler TEMPLATE_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            MetricTemplate mt = new MetricTemplate(
                    (int)r.readInt(),        // id
                    (int)r.readInt(),        // type
                    (String)r.readObject(),  // name
                    (String)r.readObject(),  // units
                    (String)r.readObject(),  // nomField
                    (String)r.readObject()); // divField

            return mt.multiply(r.readDouble())
                    .dynamicAttrs((Set<String>) r.readObject());
        }
    };


    public static final WriteHandler MARKER_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            TraceMarker m = (TraceMarker)instance;

            w.writeTag(MARKER_TAG, 4);

            w.writeInt(m.getTraceId());
            w.writeInt(m.getClock());
            w.writeInt(m.getMinimumTime());
            w.writeInt(m.getFlags());
        }
    };


    public static final ReadHandler MARKER_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            TraceMarker m = new TraceMarker((int)r.readInt(), r.readInt());

            m.setMinimumTime(r.readInt());
            m.setFlags((int)r.readInt());

            return m;
        }
    };


    public static WriteHandler EXCEPTION_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            SymbolicException e = (SymbolicException)instance;

            w.writeTag(EXCEPTION_TAG, 4);

            w.writeInt(e.getClassId());
            w.writeString(e.getMessage());
            w.writeObject(Arrays.asList(e.getStackTrace()));
            w.writeObject(e.getCause());
        }
    };


    public static ReadHandler EXCEPTION_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new SymbolicException(
                    (int)r.readInt(),
                    (String)r.readObject(),
                    ((List<SymbolicStackElement>)r.readObject()).toArray(new SymbolicStackElement[0]),
                    (SymbolicException)r.readObject());
        }
    };



    public static final WriteHandler STACKEL_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            SymbolicStackElement se = (SymbolicStackElement)instance;

            w.writeTag(STACKEL_TAG, 4);

            w.writeInt(se.getClassId());
            w.writeInt(se.getMethodId());
            w.writeInt(se.getFileId());
            w.writeInt(se.getLineNum());
        }
    };


    public static final ReadHandler STACKEL_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new SymbolicStackElement((int)r.readInt(), (int)r.readInt(), (int)r.readInt(), (int)r.readInt());
        }
    };


    public static final WriteHandler PERFRECORD_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            PerfRecord pe = (PerfRecord)instance;

            w.writeTag(PERFRECORD_TAG, 3);

            w.writeInt(pe.getClock());
            w.writeInt(pe.getScannerId());
            w.writeObject(pe.getSamples());
        }
    };


    public static final ReadHandler PERFRECORD_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new PerfRecord(r.readInt(), (int)r.readInt(), (List<PerfSample>)r.readObject());
        }
    };


    public static final WriteHandler PERFSAMPLE_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            PerfSample ps = (PerfSample)instance;

            w.writeTag(PERFSAMPLE_TAG, 3);

            w.writeInt(ps.getMetricId());
            w.writeObject(ps.getValue());
            w.writeObject(ps.getAttrs());
        }
    };


    public static final ReadHandler PERFSAMPLE_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new PerfSample((int)r.readInt(), (Number)r.readObject(), (Map<Integer,String>)r.readObject());
        }
    };


    public static ILookup<Class, Map<String,WriteHandler>> WRITE_LOOKUP =
        new ChainedLookup<Class, Map<String, WriteHandler>>(

            // Default handlers
            Handlers.defaultWriteHandlers(),

            // Handlers for Zorka-specific types
            new InheritanceLookup<Map<String, WriteHandler>>(
                new MapLookup<Class, Map<String, WriteHandler>>(
                    ZorkaUtil.<Class,Map<String,WriteHandler>>constMap(
                        Symbol.class,               ZorkaUtil.<String,WriteHandler>constMap(SYMBOL_TAG, SYMBOL_WH),
                        TraceRecord.class,          ZorkaUtil.<String,WriteHandler>constMap(RECORD_TAG, RECORD_WH),
                        Metric.class,               ZorkaUtil.<String,WriteHandler>constMap(METRIC_TAG, METRIC_WH),
                        MetricTemplate.class,       ZorkaUtil.<String,WriteHandler>constMap(TEMPLATE_TAG, TEMPLATE_WH),
                        TraceMarker.class,          ZorkaUtil.<String,WriteHandler>constMap(MARKER_TAG, MARKER_WH),
                        SymbolicException.class,    ZorkaUtil.<String,WriteHandler>constMap(EXCEPTION_TAG, EXCEPTION_WH),
                        SymbolicStackElement.class, ZorkaUtil.<String,WriteHandler>constMap(STACKEL_TAG, STACKEL_WH),
                        PerfRecord.class,           ZorkaUtil.<String,WriteHandler>constMap(PERFRECORD_TAG, PERFRECORD_WH),
                        PerfSample.class,           ZorkaUtil.<String,WriteHandler>constMap(PERFSAMPLE_TAG, PERFSAMPLE_WH)
                    ))),

            // Null handler for other types
            new ILookup<Class, Map<String, WriteHandler>>() {
                @Override public Map<String, WriteHandler> valAt(Class key) {
                    return ZorkaUtil.constMap("null", NULL_WRITE_HANDLER);
                }
            }
        );


    public static ILookup<Object, ReadHandler> READ_LOOKUP =
        new MapLookup<Object,ReadHandler>(
            ZorkaUtil.<Object,ReadHandler>constMap(
                SYMBOL_TAG,     SYMBOL_RH,
                RECORD_TAG,     RECORD_RH,
                METRIC_TAG,     METRIC_RH,
                TEMPLATE_TAG,   TEMPLATE_RH,
                MARKER_TAG,     MARKER_RH,
                EXCEPTION_TAG,  EXCEPTION_RH,
                STACKEL_TAG,    STACKEL_RH,
                PERFRECORD_TAG, PERFRECORD_RH,
                PERFSAMPLE_TAG, PERFSAMPLE_RH
        ));
}
