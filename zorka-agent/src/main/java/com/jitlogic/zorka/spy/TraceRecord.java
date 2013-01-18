/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


/**
 * Represents trace information about single method call.
 * May contain references to information about calls from this method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceRecord {

    private static final int OVERFLOW_FLAG = 1;

    private int classId, methodId, signatureId, flags;
    private long time;
    private long calls, errors;

    private TraceMarker marker;
    private TracedException exception;
    private TraceRecord parent;
    private Map<Integer,Object> attrs;
    private List<TraceRecord> children;


    public TraceRecord(TraceRecord parent) {
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


    public void addChild(TraceRecord child) {
        if (children == null) {
            children = new ArrayList<TraceRecord>();
        }
        children.add(child);
        child.parent = this;
    }


    public TraceRecord getChild(int i) {
        if (children != null && i < children.size()) {
            return children.get(i);
        } else {
            return null;
        }
    }


    public TraceRecord getParent() {
        return parent;
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
        return marker != null ? marker.getClock() : 0L;
    }


    public void setSignatureId(int signatureId) {
        this.signatureId = signatureId;
    }


    public int getTraceId() {
        return marker != null ? marker.getTraceId() : 0;
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


    public TraceMarker getMarker() {
        return marker;
    }


    public void setMarker(TraceMarker marker) {
        this.marker = marker;
    }


    public void traverse(TraceEventHandler output) {
        if (marker != null) {
            output.traceBegin(marker.getTraceId(), getClock());
        }

        output.traceEnter(classId, methodId, signatureId, 0);
        output.traceStats(calls, errors, marker != null ? marker.getFlags() : 0);

        if (attrs != null) {
            for (Map.Entry<Integer,Object> entry : attrs.entrySet()) {
                output.newAttr(entry.getKey(), entry.getValue());
            }
        }

        if (children != null) {
            for (TraceRecord child : children) {
                child.traverse(output);
            }
        }

        if (exception != null) {
            output.traceError(exception, time);
        } else {
            output.traceReturn(time);
        }
    }


    public int childCount() {
        return children != null ? children.size() : 0;
    }


    public void clean() {
        time = 0;
        classId = methodId = signatureId = 0;
        attrs = null;
        children = null;
        marker = null;
        calls = errors = 0;
        flags = 0;
    }

    public void markOverflow() {
        flags |= OVERFLOW_FLAG;
    }

    public boolean hasOverflow() {
        return 0 != (flags & OVERFLOW_FLAG);
    }

}
