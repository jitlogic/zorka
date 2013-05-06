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

package com.jitlogic.zorka.core.store;

import com.jitlogic.zorka.core.spy.TraceMarker;
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.util.ByteBuffer;

import java.util.Map;

public class TraceEntry {

    private long sid, pos, len;
    private long tstamp, time;
    private long calls, errors, recs;

    private String label;


    public TraceEntry(long sid, long pos, long len, TraceRecord record, SymbolRegistry symbols) {
        this.sid = sid;
        this.pos = pos;
        this.len = len;


        this.time = record.getTime();
        this.calls = record.getCalls();
        this.errors = record.getErrors();
        this.recs = countRecords(record);

        TraceMarker marker = record.getMarker();

        if (marker != null) {
            this.tstamp = marker.getClock();
            StringBuilder sb = new StringBuilder(96);
            sb.append(symbols.symbolName(marker.getTraceId()));
            if (record.getAttrs() != null) {
                for (Map.Entry<Integer,Object> e : record.getAttrs().entrySet()) {
                    sb.append('|');
                    Object v = e.getValue();
                    sb.append(v != null ? v.toString() : "<null>");
                }
            }
            label = sb.toString();

            if (label.length() > 64) {
                label = label.substring(0,61) + "...";
            }
        }
    }


    private int countRecords(TraceRecord record) {
        int ret = 1;

        for (int i = 0; i < record.numChildren(); i++) {
            ret += countRecords(record.getChild(i));
        }

        return ret;
    }


    public byte[] serialize() {
        ByteBuffer buf = new ByteBuffer(96);

        buf.putLong(sid); buf.putLong(pos); buf.putLong(len);
        buf.putLong(tstamp); buf.putLong(time);
        buf.putLong(calls); buf.putLong(errors); buf.putLong(recs);
        buf.putString(label);

        return buf.getContent();
    }


    public long getSid() {
        return sid;
    }


    public void setSid(long sid) {
        this.sid = sid;
    }


    public long getPos() {
        return pos;
    }


    public void setPos(long pos) {
        this.pos = pos;
    }


    public long getLen() {
        return len;
    }


    public void setLen(long len) {
        this.len = len;
    }


    public long getTstamp() {
        return tstamp;
    }


    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }


    public long getTime() {
        return time;
    }


    public void setTime(long time) {
        this.time = time;
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


    public long getRecs() {
        return recs;
    }


    public void setRecs(long recs) {
        this.recs = recs;
    }


    public String getLabel() {
        return label;
    }


    public void setLabel(String label) {
        this.label = label;
    }
}
