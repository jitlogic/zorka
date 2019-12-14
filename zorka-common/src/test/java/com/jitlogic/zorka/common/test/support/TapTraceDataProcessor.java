package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.cbor.TraceDataProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TapTraceDataProcessor implements TraceDataProcessor {

    private TraceDataProcessor next;
    private List<String> tap = new ArrayList<String>();

    public TapTraceDataProcessor() {
        this(null);
    }

    public TapTraceDataProcessor(TraceDataProcessor next) {
        this.next = next;
    }

    public boolean has(String match) {
        if (match.startsWith("~")) {
            Pattern re = Pattern.compile(match.substring(1));
            for (String t : tap) {
                if (re.matcher(t).matches()) return true;
            }
        } else {
            for (String t : tap) {
                if (t.contains(match)) return true;
            }
        }
        return false;
    }

    public List<String> getTap() {
        return tap;
    }

    public void clear() {
        tap.clear();
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        tap.add("stringRef|" + symbolId + "|" + symbol);
        if (next != null) next.stringRef(symbolId, symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        tap.add("methodRef|" + symbolId + "|" + classId  + "|" + methodId + "|" + signatureId);
        if (next != null) next.methodRef(symbolId, classId, methodId, signatureId);
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        tap.add("traceStart|" + pos + "|" + tstart + "|" + methodId);
        if (next != null) next.traceStart(pos, tstart, methodId);
    }

    @Override
    public void traceEnd(long tstop, long calls, int flags) {
        tap.add("traceEnd|" + tstop + "|" + calls + "|" + flags);
        if (next != null) next.traceEnd(tstop, calls, flags);
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        tap.add("traceBegin|" + tstamp + "|" + ttypeId + "|" + spanId + "|" + parentId);
        if (next != null) next.traceBegin(tstamp, ttypeId, spanId, parentId);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        tap.add("traceAttr|" + attrId + "|" + attrVal);
        if (next != null) next.traceAttr(attrId, attrVal);
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        tap.add("traceAttr|" + ttypeId + "|" + attrId + "|" + attrVal);
        if (next != null) next.traceAttr(ttypeId, attrId, attrVal);
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        StringBuilder sb = new StringBuilder();
        for (int[] si : stackTrace) {
            sb.append(String.format("[%d,%d,%d,%d]",si[0],si[1],si[2],si[3]));
        }
        tap.add("exception|" + excId + "|" + classId + "|" + message + "|" + cause + "|" + sb + "|" + attrs);
        if (next != null) next.exception(excId, classId, message, cause, stackTrace, attrs);
    }

    @Override
    public void exceptionRef(long excId) {
        tap.add("exceptionRef|" + excId);
        if (next != null) next.exceptionRef(excId);
    }
}
