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

public class TraceInfoSearchResult {

    public static final int MORE_RESULTS = 1;

    private int flags;

    private long lastOffs;

    private List<TraceInfo> results;

    public TraceInfoSearchResult(List<TraceInfo> results) {
        this.results = results;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void markFlag(int flag) {
        this.flags |= flag;
    }

    public long getLastOffs() {
        return lastOffs;
    }

    public void setLastOffs(long lastOffs) {
        this.lastOffs = lastOffs;
    }

    public List<TraceInfo> getResults() {
        return results;
    }

    public void setResults(List<TraceInfo> results) {
        this.results = results;
    }
}
