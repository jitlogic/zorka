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

package com.jitlogic.zorka.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TraceElement {

    private int traceId, classId, methodId, signatureId;
    private long clock, time;
    private long calls, errors;

    private TracedException exception;
    private TraceElement parent;
    private Map<Integer,Object> attrs;
    private List<TraceElement> children;


    public TraceElement(TraceElement parent) {
        this.parent = parent;
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


    public void clean() {
        time = 0;
        classId = methodId = signatureId = traceId = 0;
        attrs = null;
        children = null;
        calls = errors = 0;
    }


    public TraceElement getParent() {
        return parent;
    }


    public boolean isBusy() {
        return classId != 0;
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



    public long getClock() {
        return clock;
    }


    public void setClock(long clock) {
        this.clock = clock;
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
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }


    public TracedException getException() {
        return exception;
    }


    public void setException(TracedException exception) {
        this.exception = exception;
    }


    public void traverse(TraceEventHandler output) {
        if (traceId != 0) {
            output.traceBegin(traceId, clock);
        }

        output.traceEnter(classId, methodId, signatureId, 0);
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
            output.traceError(exception, time);
        } else {
            output.traceReturn(time);
        }
    }
}
