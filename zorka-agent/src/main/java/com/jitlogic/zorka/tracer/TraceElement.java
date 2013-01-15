/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TraceElement extends TraceEventHandler {

    private int traceId, classId, methodId, signatureId;
    private long tstart, tstop;
    private long calls, errors;

    private TracedException exception;
    private TraceElement parent;
    private Map<Integer,Object> attrs;
    private List<TraceElement> children;


    public TraceElement(TraceElement parent) {
        this.parent = parent;
    }


    @Override
    public void traceBegin(int traceId) {
        this.traceId = traceId;
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        this.classId = classId;
        this.methodId = methodId;
        this.signatureId = signatureId;
        this.tstart = tstamp;
        this.calls++;
    }


    @Override
    public void traceReturn(long tstamp) {
        this.tstop = tstamp;
    }

    @Override
    public void traceStats(long calls, long errors) {
        this.calls = calls;
        this.errors = errors;
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        this.tstop = tstamp;
        this.exception = exception;
        this.errors++;
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        setAttr(attrId, attrVal);
    }

    public Object getAttr(int attrId) {
        if (attrs != null) {
            return attrs.get(attrId);
        } else {
            return null;
        }
    }

    public void setAttr(int attrId, Object attrVal) {
        if (attrs == null) {
            attrs = new HashMap<Integer,Object>();
        }
        attrs.put(attrId, attrVal);
    }


    public void addChild(TraceElement child) {
        if (children == null) {
            children = new ArrayList<TraceElement>();
        }
        children.add(child);
    }


    public void mergeChild(TraceElement child) {
        calls += child.calls;
        errors += child.errors;
    }


    public void clean() {
        tstart = tstop = 0;
        classId = methodId = signatureId = traceId = 0;
        attrs = null;
        children = null;
        calls = errors = 0;
    }


    public TraceElement getParent() {
        return parent;
    }


    public boolean isBusy() {
        return tstart > 0;
    }


    public boolean isTrace() {
        return traceId > 0;
    }


    public boolean hasAttrs() {
        return attrs != null;
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


    public void setSignatureId(int signatureId) {
        this.signatureId = signatureId;
    }


    public int getTraceId() {
        return traceId;
    }


    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }


    public long getTime() {
        return tstop > 0 ? tstop - tstart : 0;
    }


    public TracedException getException() {
        return exception;
    }


    public void setException(TracedException exception) {
        this.exception = exception;
    }


    public void traverse(TraceEventHandler output) {
        if (traceId != 0) {
            output.traceBegin(traceId);
        }

        output.traceEnter(classId, methodId, signatureId, tstart);
        output.traceStats(calls, errors);

        if (attrs != null) {
            for (Map.Entry<Integer,Object> entry : attrs.entrySet()) {
                output.newAttr(entry.getKey(), entry.getValue());
            }
        }

        if (children != null) {
            for (TraceElement child : children) {
                child.traverse(output);
            }
        }

        if (exception != null) {
            output.traceError(exception, tstop);
        } else {
            output.traceReturn(tstop);
        }
    }
}
