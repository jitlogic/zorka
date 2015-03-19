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
package com.jitlogic.zorka.common.tracedata;


import com.jitlogic.zorka.common.util.ZorkaUtil;
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

/**
 * This class contains Fressian handlers for Zorka Trace Format. This data format
 * is used for both local file store and network stores.
 */
public class FressianTraceFormat {

    public static final String SYMBOL_TAG = "com.jitlogic.zorka.Symbol_v1";
    public static final String METRIC_TAG = "com.jitlogic.zorka.Metric_v1";
    public static final String TEMPLATE_TAG = "com.jitlogic.zorka.MetricTemplate_v1";
    public static final String RECORD_TAG = "com.jitlogic.zorka.TraceRecord_v1";
    public static final String MARKER_TAG = "com.jitlogic.zorka.TraceMarker_v1";
    public static final String EXCEPTION_TAG = "com.jitlogic.zorka.SymbolicException_v1";
    public static final String STACKEL_TAG = "com.jitlogic.zorka.SymbolicStackElement_v1";
    public static final String PERFRECORD_TAG = "com.jitlogic.zorka.PerfRecord_v1";
    public static final String PERFSAMPLE_TAG = "com.jitlogic.zorka.PerfSample_v1";
    public static final String HELLO_TAG = "com.jitlogic.zorka.HelloRequest_v1";
    public static final String TAGGED_TAG = "com.jitlogic.zorka.TaggedValue_v1";


    /**
     * Default write handler for values of unknown types.
     */
    private static final WriteHandler NULL_WRITE_HANDLER = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            w.writeNull();
        }
    };


    /**
     * Write handler for symbols.
     */
    private static final WriteHandler SYMBOL_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            Symbol s = (Symbol) instance;

            w.writeTag(SYMBOL_TAG, 2);
            w.writeInt(s.getId());
            w.writeObject(s.getName());
        }
    };


    /**
     * Read handler for symbols.
     */
    private static final ReadHandler SYMBOL_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new Symbol((int) r.readInt(), (String) r.readObject());
        }
    };


    /**
     * Write handler for trace records.
     */
    private static final WriteHandler RECORD_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            TraceRecord tr = (TraceRecord) instance;
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


    /**
     * Trace record builder interface. This can be used in clients to extend data type representing
     * trace records (eg. ViewerTraceRecord in zorka-viewer).
     */
    public static interface TraceRecordBuilder {
        TraceRecord get();
    }


    /**
     * Default trace record builder builds objects of class TraceRecord. Create new
     * builder implementation and assign it to this variable in order to change this
     * data type.
     */
    public static TraceRecordBuilder TRACE_RECORD_BUILDER = new TraceRecordBuilder() {
        @Override
        public TraceRecord get() {
            return new TraceRecord(null);
        }
    };


    /**
     * Read handler for trace records.
     */
    private static final ReadHandler RECORD_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            TraceRecord tr = TRACE_RECORD_BUILDER.get();
            tr.setClassId((int) r.readInt());
            tr.setMethodId((int) r.readInt());
            tr.setSignatureId((int) r.readInt());
            tr.setFlags((int) r.readInt());
            tr.setTime(r.readInt());
            tr.setCalls(r.readInt());
            tr.setErrors(r.readInt());
            tr.setMarker((TraceMarker) r.readObject());
            tr.setException(r.readObject());

            Map<Long, Object> m = (Map<Long, Object>) r.readObject();

            if (m != null) {
                for (Map.Entry<Long, Object> e : m.entrySet()) {
                    tr.setAttr((int) (long) e.getKey(), e.getValue());
                }
            }

            List<TraceRecord> children = (List<TraceRecord>) r.readObject();

            if (children != null) {
                for (TraceRecord c : children) {
                    c.setParent(tr);
                }
                tr.setChildren(children);
            }

            return tr;
        }
    };


    /**
     * Performance data Metric object write handler.
     */
    private static final WriteHandler METRIC_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            Metric m = (Metric) instance;

            w.writeTag(METRIC_TAG, 4);

            w.writeInt(m.getId());
            w.writeInt(m.getTemplate().getType());
            w.writeInt(m.getTemplateId());
            w.writeString(m.getName());
            w.writeString(m.getDescription());
            w.writeObject(m.getAttrs());
        }
    };


    /**
     * Performance data Metric object read handler.
     */
    private static final ReadHandler METRIC_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            int id = (int) r.readInt();
            int type = (int) r.readInt();
            int templateId = (int) r.readInt();
            String name = (String) r.readObject();
            String description = (String) r.readObject();


            Map<String, Object> attrs = (Map<String, Object>) r.readObject();

            switch (type) {
                case MetricTemplate.RAW_DATA:
                    return new RawDataMetric(id, templateId, name, description, attrs);
                case MetricTemplate.RAW_DELTA:
                    return new RawDeltaMetric(id, templateId, name, description, attrs);
                case MetricTemplate.TIMED_DELTA:
                    return new TimedDeltaMetric(id, templateId, name, description, attrs);
                case MetricTemplate.UTILIZATION:
                    return new UtilizationMetric(id, templateId, name, description, attrs);
                case MetricTemplate.WINDOWED_RATE:
                    return new WindowedRateMetric(id, templateId, name, description, attrs);
            }

            return null;
        }
    };


    /**
     * MetricTemplate write handler
     */
    private static final WriteHandler TEMPLATE_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            MetricTemplate t = (MetricTemplate) instance;

            w.writeTag(TEMPLATE_TAG, 8);

            w.writeInt(t.getId());
            w.writeInt(t.getType());
            w.writeString(t.getName());
            w.writeString(t.getDescription());
            w.writeString(t.getUnits());
            w.writeString(t.getNomField());
            w.writeString(t.getDivField());
            w.writeDouble(t.getMultiplier());
            w.writeObject(t.getDynamicAttrs());
        }
    };


    /**
     * MetricTemplate read handler
     */
    private static final ReadHandler TEMPLATE_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            MetricTemplate mt = new MetricTemplate(
                (int) r.readInt(),        // id
                (int) r.readInt(),        // type
                (String) r.readObject(),  // name
                (String) r.readObject(),  // description
                (String) r.readObject(),  // units
                (String) r.readObject(),  // nomField
                (String) r.readObject()); // divField

            return mt.multiply(r.readDouble())
                    .dynamicAttrs((Set<String>) r.readObject());
        }
    };


    /**
     * TraceMarker write handler
     */
    public static final WriteHandler MARKER_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            TraceMarker m = (TraceMarker) instance;

            w.writeTag(MARKER_TAG, 4);

            w.writeInt(m.getTraceId());
            w.writeInt(m.getClock());
            w.writeInt(m.getMinimumTime());
            w.writeInt(m.getFlags());
        }
    };


    /**
     * TraceMarker read handler
     */
    public static final ReadHandler MARKER_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            TraceMarker m = new TraceMarker((int) r.readInt(), r.readInt());

            m.setMinimumTime(r.readInt());
            m.setFlags((int) r.readInt());

            return m;
        }
    };


    /**
     * SymbolicException write handler
     */
    public static WriteHandler EXCEPTION_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            SymbolicException e = (SymbolicException) instance;

            w.writeTag(EXCEPTION_TAG, 4);

            w.writeInt(e.getClassId());
            w.writeString(e.getMessage());
            w.writeObject(Arrays.asList(e.getStackTrace()));
            w.writeObject(e.getCause());
        }
    };


    /**
     * SymbolicException read handler
     */
    public static ReadHandler EXCEPTION_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new SymbolicException(
                    (int) r.readInt(),
                    (String) r.readObject(),
                    ((List<SymbolicStackElement>) r.readObject()).toArray(new SymbolicStackElement[0]),
                    (SymbolicException) r.readObject());
        }
    };


    /**
     * Exception StackElement write handler
     */
    public static final WriteHandler STACKEL_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            SymbolicStackElement se = (SymbolicStackElement) instance;

            w.writeTag(STACKEL_TAG, 4);

            w.writeInt(se.getClassId());
            w.writeInt(se.getMethodId());
            w.writeInt(se.getFileId());
            w.writeInt(se.getLineNum());
        }
    };


    /**
     * Exception StackElement write handler
     */
    public static final ReadHandler STACKEL_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new SymbolicStackElement((int) r.readInt(), (int) r.readInt(), (int) r.readInt(), (int) r.readInt());
        }
    };


    /**
     * Performance data PerfRecord write handler
     */
    public static final WriteHandler PERFRECORD_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            PerfRecord pe = (PerfRecord) instance;

            w.writeTag(PERFRECORD_TAG, 3);

            w.writeInt(pe.getClock());
            w.writeInt(pe.getScannerId());
            w.writeObject(pe.getSamples());
        }
    };


    /**
     * Performance data PerfRecord read handler
     */
    public static final ReadHandler PERFRECORD_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new PerfRecord(r.readInt(), (int) r.readInt(), (List<PerfSample>) r.readObject());
        }
    };


    /**
     * Performance data PerfSample read handler
     */
    public static final WriteHandler PERFSAMPLE_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            PerfSample ps = (PerfSample) instance;

            w.writeTag(PERFSAMPLE_TAG, 3);

            w.writeInt(ps.getMetricId());
            w.writeObject(ps.getValue());
            w.writeObject(ps.getAttrs());
        }
    };


    /**
     * Performance data PerfSample Write Handler
     */
    public static final ReadHandler PERFSAMPLE_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new PerfSample((int) r.readInt(), (Number) r.readObject(), (Map<Integer, String>) r.readObject());
        }
    };


    /**
     * HELLO record read handler
     */
    public static final ReadHandler HELLO_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new HelloRequest(r.readInt(), (String) r.readObject(), (String) r.readObject());
        }
    };


    public static final WriteHandler HELLO_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            HelloRequest hello = (HelloRequest) instance;

            w.writeTag(HELLO_TAG, 3);
            w.writeInt(hello.getTstamp());
            w.writeObject(hello.getHostname());
            w.writeObject(hello.getAuth());
        }
    };


    /**
     * Tagged Values
     */
    public static final ReadHandler TAGGED_RH = new ReadHandler() {
        @Override
        public Object read(Reader r, Object tag, int componentCount) throws IOException {
            return new TaggedValue((int) r.readInt(), r.readObject());
        }
    };


    public static final WriteHandler TAGGED_WH = new WriteHandler() {
        @Override
        public void write(Writer w, Object instance) throws IOException {
            TaggedValue tv = (TaggedValue) instance;

            w.writeTag(TAGGED_TAG, 2);
            w.writeInt(tv.getTagId());
            w.writeObject(tv.getValue());
        }
    };


    /**
     * Lookup object grouping all write handlers
     */
    public static ILookup<Class, Map<String, WriteHandler>> WRITE_LOOKUP =
            new ChainedLookup<Class, Map<String, WriteHandler>>(

                    // Default handlers
                    Handlers.defaultWriteHandlers(),

                    // Handlers for Zorka-specific types
                    new InheritanceLookup<Map<String, WriteHandler>>(
                            new MapLookup<Class, Map<String, WriteHandler>>(
                                    ZorkaUtil.<Class, Map<String, WriteHandler>>constMap(
                                            Symbol.class, ZorkaUtil.<String, WriteHandler>constMap(SYMBOL_TAG, SYMBOL_WH),
                                            TraceRecord.class, ZorkaUtil.<String, WriteHandler>constMap(RECORD_TAG, RECORD_WH),
                                            Metric.class, ZorkaUtil.<String, WriteHandler>constMap(METRIC_TAG, METRIC_WH),
                                            MetricTemplate.class, ZorkaUtil.<String, WriteHandler>constMap(TEMPLATE_TAG, TEMPLATE_WH),
                                            TraceMarker.class, ZorkaUtil.<String, WriteHandler>constMap(MARKER_TAG, MARKER_WH),
                                            SymbolicException.class, ZorkaUtil.<String, WriteHandler>constMap(EXCEPTION_TAG, EXCEPTION_WH),
                                            SymbolicStackElement.class, ZorkaUtil.<String, WriteHandler>constMap(STACKEL_TAG, STACKEL_WH),
                                            PerfRecord.class, ZorkaUtil.<String, WriteHandler>constMap(PERFRECORD_TAG, PERFRECORD_WH),
                                            PerfSample.class, ZorkaUtil.<String, WriteHandler>constMap(PERFSAMPLE_TAG, PERFSAMPLE_WH),
                                            HelloRequest.class, ZorkaUtil.<String, WriteHandler>constMap(HELLO_TAG, HELLO_WH),
                                            TaggedValue.class, ZorkaUtil.<String, WriteHandler>constMap(TAGGED_TAG, TAGGED_WH)
                                    ))),

                    // Null handler for other types
                    new ILookup<Class, Map<String, WriteHandler>>() {
                        @Override
                        public Map<String, WriteHandler> valAt(Class key) {
                            return ZorkaUtil.constMap("null", NULL_WRITE_HANDLER);
                        }
                    }
            );


    /**
     * Lookup object grouping all read handlers
     */
    public static ILookup<Object, ReadHandler> READ_LOOKUP =
            new MapLookup<Object, ReadHandler>(
                    ZorkaUtil.<Object, ReadHandler>constMap(
                            SYMBOL_TAG, SYMBOL_RH,
                            RECORD_TAG, RECORD_RH,
                            METRIC_TAG, METRIC_RH,
                            TEMPLATE_TAG, TEMPLATE_RH,
                            MARKER_TAG, MARKER_RH,
                            EXCEPTION_TAG, EXCEPTION_RH,
                            STACKEL_TAG, STACKEL_RH,
                            PERFRECORD_TAG, PERFRECORD_RH,
                            PERFSAMPLE_TAG, PERFSAMPLE_RH,
                            HELLO_TAG, HELLO_RH,
                            TAGGED_TAG, TAGGED_RH
                    ));
}
