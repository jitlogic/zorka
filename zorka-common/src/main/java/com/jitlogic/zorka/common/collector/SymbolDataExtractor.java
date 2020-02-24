package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicMethod;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extracts symbols and methods from trace data. */
public class SymbolDataExtractor implements TraceDataProcessor {

    private SymbolRegistry registry;

    private Map<Integer,String> symbols = new HashMap<Integer, String>();
    private Map<Integer, SymbolicMethod> methods = new HashMap<Integer, SymbolicMethod>();

    public SymbolDataExtractor(SymbolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        SymbolicMethod m = registry.methodDef(methodId);
        if (m != null) {
            methods.put(methodId, m);
            String className = registry.symbolName(m.getClassId());
            if (className != null) symbols.put(m.getClassId(), className);
            String methodName = registry.symbolName(m.getMethodId());
            if (methodName != null) symbols.put(m.getMethodId(), methodName);
            String signature = registry.symbolName(m.getSignatureId());
            if (signature != null) symbols.put(m.getSignatureId(), signature);
        }
    }

    @Override
    public void traceEnd(int pos, long tstop, long calls, int flags) {
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        String ttypeName = registry.symbolName(ttypeId);
        if (ttypeName != null) symbols.put(ttypeId, ttypeName);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        String attrName = registry.symbolName(attrId);
        if (attrName != null) symbols.put(attrId, attrName);
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        String ttypeName = registry.symbolName(ttypeId);
        if (ttypeName != null) symbols.put(ttypeId, ttypeName);
        String attrName = registry.symbolName(attrId);
        if (attrName != null) symbols.put(attrId, attrName);
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        String className = registry.symbolName(classId);
        if (className != null) symbols.put(classId, className);
        if (stackTrace != null) {
            for (int[] item : stackTrace) {
                if (item != null && item.length >= 3) {
                    String clazz = registry.symbolName(item[0]);
                    if (clazz != null) symbols.put(item[0], clazz);
                    String method = registry.symbolName(item[1]);
                    if (method != null) symbols.put(item[1], method);
                    String fname = registry.symbolName(item[2]);
                    if (fname != null) symbols.put(item[2], fname);
                }
            }
        }
    }

    @Override
    public void exceptionRef(long excId) {
    }

    public byte[] getSymbolData() {
        CborDataWriter cbw = new CborDataWriter(1024,1024);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        for (Map.Entry<Integer,String> e : symbols.entrySet()) {
            tdw.stringRef(e.getKey(), e.getValue());
        }
        for (Map.Entry<Integer,SymbolicMethod> e : methods.entrySet()) {
            SymbolicMethod sm = e.getValue();
            tdw.methodRef(e.getKey(), sm.getClassId(), sm.getMethodId(), sm.getSignatureId());
        }
        return cbw.toByteArray();
    }

    public static byte[] extractSymbolData(SymbolRegistry registry, byte[] tdata) {
        CborDataReader cdr = new CborDataReader(tdata);
        SymbolDataExtractor sdep = new SymbolDataExtractor(registry);
        new TraceDataReader(cdr, sdep).run();
        return ZorkaUtil.gzip(sdep.getSymbolData());
    }
}
