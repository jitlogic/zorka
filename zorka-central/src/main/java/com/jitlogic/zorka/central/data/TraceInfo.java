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
package com.jitlogic.zorka.central.data;


import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

public class TraceInfo {

    @JsonProperty
    int hostId;

    @JsonProperty
    long dataOffs;

    @JsonProperty
    int traceId;

    @JsonProperty
    int dataLen;

    @JsonProperty
    long clock;

    @JsonProperty
    int methodFlags;

    @JsonProperty
    int traceFlags;

    @JsonProperty
    int status;

    @JsonProperty
    int classId;

    @JsonProperty
    int methodId;

    @JsonProperty
    int signatureId;

    @JsonProperty
    long calls;

    @JsonProperty
    long errors;

    @JsonProperty
    long records;

    @JsonProperty
    long executionTime;

    @JsonProperty
    String description;

    @JsonProperty
    Map<String, String> attributes;

    @JsonProperty
    SymbolicExceptionInfo exceptionInfo;

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }

    public long getDataOffs() {
        return dataOffs;
    }

    public void setDataOffs(long dataOffs) {
        this.dataOffs = dataOffs;
    }

    public int getTraceId() {
        return traceId;
    }

    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public long getClock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public int getMethodFlags() {
        return methodFlags;
    }

    public void setMethodFlags(int methodFlags) {
        this.methodFlags = methodFlags;
    }

    public int getTraceFlags() {
        return traceFlags;
    }

    public void setTraceFlags(int traceFlags) {
        this.traceFlags = traceFlags;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public long getRecords() {
        return records;
    }

    public void setRecords(long records) {
        this.records = records;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public SymbolicExceptionInfo getExceptionInfo() {
        return exceptionInfo;
    }

    public void setExceptionInfo(SymbolicExceptionInfo exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }
}
