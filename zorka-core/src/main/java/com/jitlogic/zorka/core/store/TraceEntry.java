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


import com.jitlogic.zorka.core.util.ZorkaUtil;

import java.io.Serializable;

public class TraceEntry implements Serializable {

    public static final int FORMAT_NULL     = 0;
    public static final int FORMAT_SIMPLE   = 1;
    public static final int FORMAT_ASN1_BER = 2;
    public static final int FORMAT_ASN1_PER = 3;

    private long id;
    private int format;
    private long pos, len;
    private long tstamp, time;
    private long calls, errors, recs;

    private String label;


    public TraceEntry() {
    }


    public TraceEntry(long id, int format, long pos, long len, long tstamp, long time, long calls, long errors, long recs, String label) {
        this.id = id;
        this.format = format;
        this.pos = pos;
        this.len = len;
        this.tstamp = tstamp;
        this.time = time;
        this.calls = calls;
        this.errors = errors;
        this.recs = recs;
        this.label = label;

    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TraceEntry && ((TraceEntry)obj).id == id;
    }

    @Override
    public int hashCode() {
        return (int)(id * 31);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getFormat() {
        return format;
    }


    public void setFormat(int format) {
        this.format = format;
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
