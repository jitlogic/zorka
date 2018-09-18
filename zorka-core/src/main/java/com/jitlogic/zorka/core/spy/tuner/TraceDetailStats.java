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

    public static final long CALL_MASK = 0x0000000000FFFFFFL;
    private static final long CALL_INV = 0xFFFFFFFFFF000000L;
    private static final long CALL_MAX = 0xFFFFFFL;

    public static final long DROP_BITS = 24;
    public static final long DROP_MASK = 0x0000FFFFFF000000L;
    private static final long DROP_INV  = 0xFFFF000000FFFFFFL;
    private static final long DROP_MAX  = 0xFFFFFFL;

    public static final long ERR_BITS = 48;
    public static final long ERR_MASK = 0x00FF000000000000L;
    private static final long ERR_INV  = 0xFF00FFFFFFFFFFFFL;
    private static final long ERR_MAX  = 0xFFL;

    public static final long LONG_BITS = 56;
    public static final long LONG_MASK = 0xFF00000000000000L;
    private static final long LONG_INV  = 0x00FFFFFFFFFFFFFFL;
    private static final long LONG_MAX  = 0xFFL;

    private int size;
    private long[] stats;

    public TraceDetailStats(int size) {
        this.size = size;
        this.stats = new long[size];
    }

    public int getSize() {
        return size;
    }

    public long[] getStats() {
        return stats;
    }

    public boolean markCall(int mid) {
        if (mid >= size) {
            stats = ZorkaUtil.clipArray(stats, ((mid + 1023) >>> 10) << 10);
        }
        long l = stats[mid];
        long c = (l & CALL_MASK) + 1;
        if (c <= CALL_MAX) {
            stats[mid] = (l & CALL_INV) | c;
            return true;
        } else {
            return false;
        }
    }

    public long getCalls(int mid) {
        if (mid > size) return 0;
        long rslt = stats[mid] & CALL_MASK;
        return rslt;
    }

    public boolean markDrop(int mid) {
        if (mid >= size) {
            stats = ZorkaUtil.clipArray(stats, ((mid + 1023) >>> 10) << 10);
        }
        long l = stats[mid];
        long c = ((l & DROP_MASK) >>> DROP_BITS) + 1;
        if (c <= DROP_MAX) {
            stats[mid] = (l & DROP_INV) | (c << DROP_BITS);
            return true;
        } else {
            return false;
        }
    }

    public long getDrops(int mid) {
        if (mid > size) return 0;
        long l = (stats[mid] & DROP_MASK) >>> DROP_BITS;
        return l;
    }

    public boolean markError(int mid) {
        if (mid >= size) {
            stats = ZorkaUtil.clipArray(stats, ((mid + 1023) >>> 10) << 10);
        }
        long l = stats[mid];
        long c = ((l & ERR_MASK) >>> ERR_BITS) + 1;
        if (c <= ERR_MAX) {
            stats[mid] = (l & ERR_INV) | (c << ERR_BITS);
            return true;
        } else {
            return false;
        }
    }

    public long getErrors(int mid) {
        if (mid > size) return 0;
        return (stats[mid] & ERR_MASK) >>> ERR_BITS;
    }

    public boolean markLCall(int mid) {
        if (mid >= size) {
            stats = ZorkaUtil.clipArray(stats, ((mid + 1023) >>> 10) << 10);
        }
        long l = stats[mid];
        long c = ((l & LONG_MASK) >>> LONG_BITS) + 1;
        if (c <= LONG_MAX) {
            stats[mid] = (l & LONG_INV) | (c << LONG_BITS);
            return true;
        } else {
            return false;
        }
    }

    public long getLCalls(int mid) {
        if (mid > size) return 0;
        return (stats[mid] & LONG_MASK) >>> LONG_BITS;
    }
}
