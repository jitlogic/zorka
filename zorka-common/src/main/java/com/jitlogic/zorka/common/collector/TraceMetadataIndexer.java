package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceRecordFlags;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Extracts additional metadata needed to properly index trace. */
public class TraceMetadataIndexer implements TraceDataProcessor {

    private int level = 0;

    private TraceChunkData chunk;
    private SymbolRegistry registry;
    private TraceDataProcessor output;

    private Set<Integer> methods;


    public TraceMetadataIndexer(SymbolRegistry registry, TraceChunkData chunk, TraceDataProcessor output) {
        this.chunk = chunk;
        this.registry = registry;
        this.methods = chunk.getMethods();
        this.output = output;
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        if (output != null) output.stringRef(symbolId, symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        if (output != null) output.methodRef(symbolId, classId, methodId, signatureId);
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        level++;
        methods.add(methodId);
        chunk.setTstart(Math.min(tstart, chunk.getTstart()));
        chunk.addCalls(1);
        if (output != null) output.traceStart(pos, tstart, methodId);
    }

    @Override
    public void traceEnd(long tstop, long calls, int flags) {
        // TODO end position by się przydalo tutaj ...
        level--;
        chunk.setTstop(Math.max(tstop, chunk.getTstop()));
        chunk.addCalls((int)calls+1);
        if (0 != (flags & TraceRecordFlags.TF_ERROR_MARK)) chunk.addErrors(1);
        if (output != null) output.traceEnd(tstop, calls, flags);
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        if (output != null) output.traceBegin(tstamp, ttypeId, spanId, parentId);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        String attrName = registry.symbolName(attrId);
        // TODO to powinien być poprawny stack z listą chunków na wyjściu
        if (level == 1 && attrName != null) chunk.getAttrs().put(attrName, ""+attrVal);
        if (output != null) output.traceAttr(attrId, attrVal);
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        String attrName = registry.symbolName(attrId);
        // TODO to powinien być poprawny stack z listą chunków na wyjściu
        // TODO uwzględnić ttypeId i awans w górę stacku
        if (level == 1 && attrName != null) chunk.getAttrs().put(attrName, ""+attrVal);
        if (output != null) output.traceAttr(attrId, attrVal);

    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        // TODO wyciągnąć dane do zaindeksowania
        if (output != null) output.exception(excId, classId, message, cause, stackTrace, attrs);
    }

    @Override
    public void exceptionRef(long excId) {
        // TODO wyciągnąć dane do zaindeksowania
        if (output != null) output.exceptionRef(excId);
    }
}
