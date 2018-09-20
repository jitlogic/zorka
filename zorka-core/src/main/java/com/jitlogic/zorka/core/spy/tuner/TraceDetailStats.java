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

import com.jitlogic.zorka.common.util.ZorkaUtil;

public class TraceDetailStats {

    private int size;
    private int[] stats;

    public TraceDetailStats(int size) {
        this.size = size;
        this.stats = new int[size];
    }

    public void clear() {
        for (int i = 0; i < stats.length; i++) stats[i] = 0;
    }

    public int getSize() {
        return size;
    }

    public int[] getStats() {
        return stats;
    }

    public boolean markRank(int mid, int delta) {
        if (mid >= size) {
            stats = ZorkaUtil.clipArray(stats, ((mid + 1023) >>> 10) << 10);
            size = stats.length;
        }

        stats[mid] = Math.max(stats[mid]+delta, 0);

        return true;
    }

    public long getRank(int mid) {
        return mid < size ? stats[mid] : 0;
    }

    @Override
    public String toString() {
        int used = 0;
        for (int i = 0; i < size; i++)
            if (stats[i] != 0)
                used++;
        return "TD(sz=" + size + ", used=" + used + ")";
    }
}
