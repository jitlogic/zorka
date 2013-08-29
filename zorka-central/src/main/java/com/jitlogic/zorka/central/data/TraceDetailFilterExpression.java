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


public class TraceDetailFilterExpression {

    @JsonProperty
    int hostId;

    @JsonProperty
    long traceOffs;

    @JsonProperty
    long minMethodTime;

    @JsonProperty
    int flags;


    public long getMinMethodTime() {
        return minMethodTime;
    }


    public void setMinMethodTime(long minMethodTime) {
        this.minMethodTime = minMethodTime;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }


    public int getHostId() {
        return hostId;
    }


    public void setHostId(int hostId) {
        this.hostId = hostId;
    }


    public long getTraceOffs() {
        return traceOffs;
    }


    public void setTraceOffs(long traceOffs) {
        this.traceOffs = traceOffs;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof TraceDetailFilterExpression
                && ((TraceDetailFilterExpression) obj).hostId == hostId
                && ((TraceDetailFilterExpression) obj).traceOffs == traceOffs
                && ((TraceDetailFilterExpression) obj).flags == flags
                && ((TraceDetailFilterExpression) obj).minMethodTime == minMethodTime;
    }
}
