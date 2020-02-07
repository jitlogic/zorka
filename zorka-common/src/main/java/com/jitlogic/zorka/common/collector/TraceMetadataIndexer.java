package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.BufferedTraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceRecordFlags;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicMethod;
import com.jitlogic.zorka.common.tracedata.TraceMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Extracts additional metadata needed to properly index trace. */
public class TraceMetadataIndexer implements TraceDataProcessor {

    /** Current call stack depth */
    private int stackDepth = 0;

    /** Start position (of currently processed chunk) */
    private int startOffs = 0;

    /** Current position (in currently processed chunk) */
    private int currentPos = 0;

    /** Last noticed method ID */
    private int lastMethodId;

    private long traceId1;

    private long traceId2;

    private int chunkNum;

    /** Indexing result */
    private List<TraceChunkData> result = new ArrayList<TraceChunkData>();

    /** Top of trace stack */
    private TraceChunkData top;

    private SymbolRegistry registry;
    private BufferedTraceDataProcessor output;

    /** Last received start time */
    private long tstart = Long.MAX_VALUE;

    /** Last received stop time */
    private long tstop = Long.MIN_VALUE;

    private Map<Long,TraceDataResultException> exceptions = new TreeMap<Long, TraceDataResultException>();

    public TraceMetadataIndexer(SymbolRegistry registry, BufferedTraceDataProcessor output) {
        this.registry = registry;
        this.output = output;
    }

    public void init(long traceId1, long traceId2, int chunkNum) {
        startOffs += currentPos;
        this.traceId1 = traceId1;
        this.traceId2 = traceId2;
        this.chunkNum = chunkNum;
    }

    public List<TraceChunkData> getChunks() {
        return result;
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        if (output != null) output.stringRef(symbolId, symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        if (output != null) output.methodRef(symbolId, classId, methodId, signatureId);
    }

    /** Trace push */
    private void push(TraceChunkData tcd) {
        if (top != null) tcd.setParent(top);
        top = tcd;
    }

    /** Trace pop */
    private void pop(int pos) {
        if (top != null) {
            TraceChunkData c = top;
            c.setTstart(tstart);
            c.setTstop(tstop);
            if (c.getParentId() == 0 && top.getParent() != null) {
                c.setParentId(top.getParent().getSpanId());
            }
            int len = pos - c.getStartOffs();
            if (len > 0) {
                c.setTraceData(output.chunk(c.getStartOffs(), len));
            }
            // TODO uwaga: tylko zakończone fragmenty są zapisywane; zaimplementować tymczasowy zapis niezakończonych fragmentów;
            if (top.getParent() == null || !top.hasFlag(TraceMarker.SENT_MARK)) {
                result.add(c);
            }
            top = top.getParent();
            if (top != null) {
                top.addRecs(c.getRecs());
                top.addErrors(c.getErrors());
                top.addCalls(c.getCalls());
            }
        }
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        if (output != null) pos = output.size();
        stackDepth++;
        this.currentPos = pos;
        this.tstart = tstart;
        this.lastMethodId = methodId;
        if (top != null) top.addMethod(methodId);
        if (output != null) output.traceStart(pos, tstart, methodId);
    }

    @Override
    public void traceEnd(int pos, long tstop, long calls, int flags) {
        if (output != null) {
            output.traceEnd(pos, tstop, calls, flags);
            pos = output.size();
        }
        this.currentPos = pos;
        this.tstop = tstop;
        if (top != null) {
            top.addCalls((int)calls+1);
            top.addRecs(1);
            top.setFlags(flags);
            if (0 != (flags & TraceMarker.ERROR_MARK)) {
                top.addErrors(1);
                if (top.getStackDepth() == stackDepth) top.setError(true);
            }
            if (top.getStackDepth() == stackDepth) {
                top.setTstop(tstop);
                top.setDuration(top.getTstop()-top.getTstart());
                pop(pos);
            }
        }
        stackDepth--;
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        TraceChunkData c = new TraceChunkData(traceId1, traceId2,
            parentId != 0 ? parentId : (top != null ? top.getSpanId() : 0),
            spanId, chunkNum);
        c.setStackDepth(stackDepth);
        c.setTstart(tstart);
        c.setTstamp(tstamp);
        c.setStartOffs(this.currentPos);
        c.addMethod(lastMethodId);
        SymbolicMethod mdef = registry.methodDef(lastMethodId);
        c.setKlass(registry.symbolName(mdef.getClassId()));
        c.setMethod(registry.symbolName(mdef.getMethodId()));
        c.setTtypeId(ttypeId);
        c.setTtype(registry.symbolName(ttypeId));
        push(c);

        if (output != null) output.traceBegin(tstamp, ttypeId, spanId, parentId);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        //System.out.println("Attr: " + registry.symbolName(attrId) + "(" + attrId + ") -> " + attrVal);
        String attrName = registry.symbolName(attrId);
        if (attrName != null && top != null) top.getAttrs().put(attrName, ""+attrVal);
        if (output != null) output.traceAttr(attrId, attrVal);
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        String attrName = registry.symbolName(attrId);
        for (TraceChunkData c = top; c != null; c = c.getParent()) {
            if (c.getTtypeId() == ttypeId) {
                c.getAttrs().put(attrName, ""+attrVal);
            }
        }
        if (output != null) output.traceAttr(attrId, attrVal);
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        TraceDataResultException ex = new TraceDataResultException(excId, registry.symbolName(classId), message);
        for (int[] si : stackTrace) {
            ex.getStack().add(String.format("%s.%s (%s:%d)", registry.symbolName(si[0]),
                registry.symbolName(si[1]), registry.symbolName(si[2]), si[3]));
        }
        exceptions.put(ex.getId(), ex);
        if (top != null && top.getStackDepth() == stackDepth) top.setException(ex);
        if (output != null) output.exception(excId, classId, message, cause, stackTrace, attrs);
    }

    @Override
    public void exceptionRef(long excId) {
        if (top != null && top.getStackDepth() == stackDepth && exceptions.containsKey(excId)) {
            top.setException(exceptions.get(excId));
        }
        if (output != null) output.exceptionRef(excId);
    }
}
