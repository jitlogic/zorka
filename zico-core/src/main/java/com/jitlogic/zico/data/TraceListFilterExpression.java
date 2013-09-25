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
package com.jitlogic.zico.data;


import org.codehaus.jackson.annotate.JsonProperty;

public class TraceListFilterExpression {

    @JsonProperty
    String sortBy;

    @JsonProperty
    boolean sortAsc;

    @JsonProperty
    boolean errorsOnly;

    @JsonProperty
    long minTime;

    @JsonProperty
    int traceId;

    @JsonProperty
    long timeStart;

    @JsonProperty
    long timeEnd;

    @JsonProperty
    String filterExpr;


    public String getSortBy() {
        return sortBy;
    }


    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }


    public boolean isSortAsc() {
        return sortAsc;
    }


    public void setSortAsc(boolean sortAsc) {
        this.sortAsc = sortAsc;
    }


    public boolean isErrorsOnly() {
        return errorsOnly;
    }


    public void setErrorsOnly(boolean errorsOnly) {
        this.errorsOnly = errorsOnly;
    }


    public long getMinTime() {
        return minTime;
    }


    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }


    public int getTraceId() {
        return traceId;
    }


    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }


    public String getFilterExpr() {
        return filterExpr;
    }


    public void setFilterExpr(String filterExpr) {
        this.filterExpr = filterExpr;
    }


    public long getTimeStart() {
        return timeStart;
    }


    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }


    public long getTimeEnd() {
        return timeEnd;
    }


    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }
}
