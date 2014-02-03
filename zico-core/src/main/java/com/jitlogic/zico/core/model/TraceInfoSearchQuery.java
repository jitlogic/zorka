/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zorka.common.util.ZorkaUtil;

public class TraceInfoSearchQuery {

    private int seq;

    private String hostName;

    private int flags;

    private long offset;

    private int limit;

    private String traceName;

    private long minMethodTime;

    private String searchExpr;


    public int getSeq() {
        return seq;
    }


    public void setSeq(int seq) {
        this.seq = seq;
    }


    public String getHostName() {
        return hostName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean hasFlag(int flag) {
        return 0 != (this.flags & flag);
    }


    public long getOffset() {
        return offset;
    }


    public void setOffset(long offset) {
        this.offset = offset;
    }


    public int getLimit() {
        return limit;
    }


    public void setLimit(int limit) {
        this.limit = limit;
    }


    public String getTraceName() {
        return traceName;
    }


    public void setTraceName(String traceName) {
        this.traceName = traceName;
    }


    public long getMinMethodTime() {
        return minMethodTime;
    }


    public void setMinMethodTime(long minMethodTime) {
        this.minMethodTime = minMethodTime;
    }


    public String getSearchExpr() {
        return searchExpr;
    }


    public void setSearchExpr(String searchExpr) {
        this.searchExpr = searchExpr;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof TraceInfoSearchQuery
                && ZorkaUtil.objEquals(((TraceInfoSearchQuery) obj).hostName, hostName)
                && ((TraceInfoSearchQuery) obj).offset == offset
                && ((TraceInfoSearchQuery) obj).minMethodTime == minMethodTime;
    }


    @Override
    public int hashCode() {
        return 31 * hostName.hashCode() + 17 * (int) offset + 19 * (int) minMethodTime;
    }


    @Override
    public String toString() {
        return "TraceSearchQuery(" + hostName.hashCode() + "," + offset + "," + minMethodTime + ")";
    }
}
