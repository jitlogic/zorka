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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.util.LinkedHashMap;
import java.util.Map;

public class TraceEntry {

    private long offs, len;
    private long clock;

    private int flags, tflags;
    private long calls, errors, records;
    private long time;

    private String label;

    private String desc;
    private Map<String,String> attrs;


    public TraceEntry() {
    }


    public TraceEntry(long offs, long len, SymbolSet symbolSet, TraceRecord rec) {
        this.offs = offs;
        this.len = len;
        this.clock = rec.getClock();
        this.flags = rec.getFlags();
        this.tflags = rec.getMarker().getFlags();
        this.calls = rec.getCalls();
        this.errors = rec.getErrors();
        this.records = numRecords(rec);
        this.time = rec.getTime();
        this.label = symbolSet.get(rec.getMarker().getTraceId());
        this.desc = this.label;
        if (rec.getAttrs() != null) {
            attrs = new LinkedHashMap<String, String>();
            for (Map.Entry<Integer,Object> e : rec.getAttrs().entrySet()) {
                attrs.put(symbolSet.get(e.getKey()), ""+e.getValue());
                this.desc += "|" + e.getValue();
            }
        }
    }

    private int numRecords(TraceRecord record) {
        int recs = 1;

        if (record.getChildren() != null) {
            for (TraceRecord c : record.getChildren()) {
                recs += numRecords(c);
            }
        }

        return recs;
    }

    public long getOffs() {
        return offs;
    }

    public void setOffs(long offs) {
        this.offs = offs;
    }

    public long getLen() {
        return len;
    }

    public void setLen(long len) {
        this.len = len;
    }

    public long getClock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, String> attrs) {
        this.attrs = attrs;
    }
}
