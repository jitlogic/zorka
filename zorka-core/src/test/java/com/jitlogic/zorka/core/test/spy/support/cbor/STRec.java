package com.jitlogic.zorka.core.test.spy.support.cbor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Decoded trace record. This class is implemented only for test purposes.
 */
public class STRec {
    private int mid, classId, methodId, signatureId;

    private String className, methodName, signature;

    private long tstart, tstop;
    private long calls, errors;

    private STBeg begin;
    private STErr error;

    private Map<String,Object> attrs;
    private List<STRec> children;

    private List<STAttr> uattrs;

    public STRec(Map<String,Object> attrs, List<STAttr> uattrs, List<STRec> children) {
        this.attrs = attrs;
        this.uattrs = uattrs;
        this.children = children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("T(");

        sb.append(className);
        sb.append(".");
        sb.append(methodName);

        if (begin != null) {
            sb.append(", begin=");
            sb.append(begin);
        }

        sb.append(", calls=");
        sb.append(calls);
        sb.append(", errors=");
        sb.append(errors);

        sb.append(", atrs=");
        sb.append(attrs);

        sb.append(", children=");
        sb.append(children);

        if (uattrs.size() > 0) {
            sb.append(", uattrs=");
            sb.append(uattrs);
        }

        sb.append(")");

        return sb.toString();
    }

    public void promoteUpAttrs() {
        for (STRec tr : children) {
            tr.promoteUpAttrs();
            for (STAttr ua : tr.uattrs) {
                if (begin != null && (ua.getTraceId() == 0 || ua.getTraceId() == begin.getTraceId())) {
                    attrs.putAll(ua.getAttrs());
                } else {
                    uattrs.add(ua);
                }
            }
            tr.uattrs = new ArrayList<STAttr>();
        }

        Iterator<STAttr> ir = uattrs.iterator();

        while (ir.hasNext()) {
            STAttr ua = ir.next();
            if (begin != null && (ua.getTraceId() == 0 || ua.getTraceId() == begin.getTraceId())) {
                attrs.putAll(ua.getAttrs());
                ir.remove();
            }
        }
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getMethodId() {
        return methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public int getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(int signatureId) {
        this.signatureId = signatureId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getTstart() {
        return tstart;
    }

    public void setTstart(long tstart) {
        this.tstart = tstart;
    }

    public long getTstop() {
        return tstop;
    }

    public void setTstop(long tstop) {
        this.tstop = tstop;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    public List<STRec> getChildren() {
        return children;
    }

    public void setChildren(List<STRec> children) {
        this.children = children;
    }

    public STBeg getBegin() {
        return begin;
    }

    public void setBegin(STBeg begin) {
        this.begin = begin;
    }

    public STErr getError() {
        return error;
    }

    public void setError(STErr error) {
        this.error = error;
    }
}
