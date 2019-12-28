package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.cbor.CborDataWriter;
import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceDataWriter;

public class TraceBuildUtil {

    public interface TraceBuildBlock {
        void visit(TraceDataProcessor tdp);
    }

    public static TraceBuildBlock sref(final int symbolId, final String symbol) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.stringRef(symbolId, symbol);
            }
        };
    }

    public static TraceBuildBlock mref(final int symbolId, final int classId, final int methodId, final int signatureId) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.methodRef(symbolId, classId, methodId, signatureId);
            }
        };
    }

    public static TraceBuildBlock start(final int pos, final int tstart, final int methodId, final TraceBuildBlock...tbs) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.traceStart(pos, tstart, methodId);
                for (TraceBuildBlock b : tbs) {
                    b.visit(tdp);
                }
            }
        };
    }

    public static TraceBuildBlock end(final int pos, final long tstop, final long calls, final int flags) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.traceEnd(pos, tstop, calls, flags);
            }
        };
    }

    public static TraceBuildBlock begin(final long tstamp, final int ttypeId, final long spanId, final long parentId) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.traceBegin(tstamp, ttypeId, spanId, parentId);
            }
        };
    }

    public static TraceBuildBlock attr(final int attrId, final Object attrVal) {
        return new TraceBuildBlock() {
            @Override
            public void visit(TraceDataProcessor tdp) {
                tdp.traceAttr(attrId, attrVal);
            }
        };
    }

    public static byte[] trace(TraceBuildBlock...blocks) {
        CborDataWriter cbw = new CborDataWriter(512,512);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        for (TraceBuildBlock b : blocks) b.visit(tdw);
        return cbw.toByteArray();
    }
}
