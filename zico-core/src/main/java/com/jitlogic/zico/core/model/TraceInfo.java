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
package com.jitlogic.zico.core.model;


import java.util.List;

public class TraceInfo {

    String hostName;

    long dataOffs;

    int traceId;

    String traceType;

    int dataLen;

    long clock;

    int methodFlags;

    int traceFlags;

    int status;

    int classId;

    int methodId;

    int signatureId;

    long calls;

    long errors;

    long records;

    long executionTime;

    String description;

    List<KeyValuePair> attributes;

    SymbolicExceptionInfo exceptionInfo;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
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

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
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
