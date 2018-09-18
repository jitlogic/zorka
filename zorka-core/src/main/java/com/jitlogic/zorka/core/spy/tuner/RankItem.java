/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.tuner;

public class RankItem {

    private int mid;
    private int rank;
    private long calls;
    private long drops;

    public RankItem(int mid, int rank, long calls, long drops) {
        this.mid = mid;
        this.rank = rank;
        this.calls = calls;
        this.drops = drops;
    }

    public int getMid() {
        return mid;
    }

    public int getRank() {
        return rank;
    }

    public long getCalls() {
        return calls;
    }

    public long getDrops() {
        return drops;
    }

    @Override
    public String toString() {
        return "(m=" + mid + ",r=" + rank + ",c=" + calls + ",d=" + drops + ")";
    }
}
