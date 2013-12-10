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

import com.jitlogic.zico.core.model.TraceRecordInfo;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class TraceRecordSearchResult {

    @JsonProperty
    List<TraceRecordInfo> result;

    @JsonProperty
    double sumPct;

    @JsonProperty
    double recurPct;

    @JsonProperty
    long sumTime;

    @JsonProperty
    long recurTime;

    @JsonProperty
    long minTime;

    @JsonProperty
    long maxTime;

    public List<TraceRecordInfo> getResult() {
        return result;
    }

    public void setResult(List<TraceRecordInfo> result) {
        this.result = result;
    }

    public double getSumPct() {
        return sumPct;
    }

    public void setSumPct(double sumPct) {
        this.sumPct = sumPct;
    }

    public double getRecurPct() {
        return recurPct;
    }

    public void setRecurPct(double recurPct) {
        this.recurPct = recurPct;
    }

    public long getSumTime() {
        return sumTime;
    }

    public void setSumTime(long sumTime) {
        this.sumTime = sumTime;
    }

    public long getRecurTime() {
        return recurTime;
    }

    public void setRecurTime(long recurTime) {
        this.recurTime = recurTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }
}
