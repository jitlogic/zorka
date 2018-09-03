package com.jitlogic.zorka.core.test.spy.support.cbor;

import java.util.Map;

public class STAttr {

    private int traceId;
    private String traceName;
    private Map<String,Object> attrs;

    public STAttr(int traceId, String traceName, Map<String,Object> attrs) {
        this.traceId = traceId;
        this.traceName = traceName;
        this.attrs = attrs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("U(");
        sb.append(traceName);
        sb.append(",");
        sb.append(attrs);
        sb.append(")");

        return sb.toString();
    }

    public int getTraceId() {
        return traceId;
    }

    public String getTraceName() {
        return traceName;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

}
