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
package com.jitlogic.zico.core;


import com.jitlogic.zico.core.KeyValuePair;
import com.jitlogic.zico.core.SymbolicExceptionInfo;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class TraceRecordInfo {
    @JsonProperty
    long calls;

    @JsonProperty
    long errors;

    @JsonProperty
    long time;

    @JsonProperty
    int flags;

    @JsonProperty
    String method;

    @JsonProperty
    int children;

    @JsonProperty
    String path;

    @JsonProperty
    List<KeyValuePair> attributes;

    @JsonProperty
    SymbolicExceptionInfo exceptionInfo;

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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getChildren() {
        return children;
    }

    public void setChildren(int children) {
        this.children = children;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<KeyValuePair> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<KeyValuePair> attributes) {
        this.attributes = attributes;
    }

    public SymbolicExceptionInfo getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(SymbolicExceptionInfo exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }
}
