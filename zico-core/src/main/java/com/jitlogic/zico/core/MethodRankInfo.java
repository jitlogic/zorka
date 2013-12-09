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
package com.jitlogic.zico.core;

import org.codehaus.jackson.annotate.JsonProperty;

public class MethodRankInfo {

    @JsonProperty
    long calls;

    @JsonProperty
    long errors;

    @JsonProperty
    long time;

    @JsonProperty
    long bareTime;

    @JsonProperty
    long minTime;

    @JsonProperty
    long maxTime;

    @JsonProperty
    long minBareTime;

    @JsonProperty
    long maxBareTime;

    @JsonProperty
    String method;


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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getAvgTime() {
        return calls > 0 ? time / calls : 0;
    }

    public void setAvgTime(long avgTime) {
        time = calls * avgTime;
    }

    public String getMethod() {
        return method;
    }

    public long getBareTime() {
        return bareTime;
    }

    public void setBareTime(long bareTime) {
        this.bareTime = bareTime;
    }

    public long getAvgBareTime() {
        return calls > 0 ? bareTime / calls : 0;
    }

    public void setAvgBareTime(long avgBareTime) {
        bareTime = calls * avgBareTime;
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

    public long getMinBareTime() {
        return minBareTime;
    }

    public void setMinBareTime(long minBareTime) {
        this.minBareTime = minBareTime;
    }

    public long getMaxBareTime() {
        return maxBareTime;
    }

    public void setMaxBareTime(long maxBareTime) {
        this.maxBareTime = maxBareTime;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
