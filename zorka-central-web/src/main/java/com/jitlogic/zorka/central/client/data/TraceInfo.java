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
package com.jitlogic.zorka.central.client.data;


import org.codehaus.jackson.annotate.JsonProperty;

public class TraceInfo {

    @JsonProperty
    int HOST_ID;

    @JsonProperty
    long DATA_OFFS;

    @JsonProperty
    int TRACE_ID;

    @JsonProperty
    int DATA_LEN;

    @JsonProperty
    long CLOCK;

    @JsonProperty
    int RFLAGS;

    @JsonProperty
    int TFLAGS;

    @JsonProperty
    long CALLS;

    @JsonProperty
    long ERRORS;

    @JsonProperty
    long RECORDS;

    @JsonProperty
    long EXTIME;

    @JsonProperty
    String OVERVIEW;

    public int getHostId() {
        return HOST_ID;
    }

    public long getDataOffs() {
        return DATA_OFFS;
    }

    public int getTraceId() {
        return TRACE_ID;
    }

    public int getDataLen() {
        return DATA_LEN;
    }

    public long getClock() {
        return CLOCK;
    }

    public int getMethodFlags() {
        return RFLAGS;
    }

    public int getTraceFlags() {
        return TFLAGS;
    }

    public long getCalls() {
        return CALLS;
    }

    public long getErrors() {
        return ERRORS;
    }

    public long getRecords() {
        return RECORDS;
    }

    public long getExecutionTime() {
        return EXTIME;
    }

    public String getDescription() {
        return OVERVIEW;
    }
}

