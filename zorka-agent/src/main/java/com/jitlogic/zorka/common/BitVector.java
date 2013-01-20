/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common;

public class BitVector {

    private long bits[];
    private int len;
    private int delta;


    public BitVector() {
        this(512);
    }


    public BitVector(int initial) {
        len = delta = initial;
        bits = new long[initial];
    }


    public boolean get(int bit) {
        int idx = bit >> 6, off = bit & 63;
        return idx < len ? 0 != (bits[idx] & (1L << off)) : false;
    }


    public void set(int bit) {
        int idx = bit >> 6, off = bit & 63;

        if (idx >= len) {
            int l = len;
            while (l <= idx) {
                l += delta;
            }
            long[] nbits = new long[l];
            System.arraycopy(bits, 0, nbits, 0, len);
            bits = nbits;
            len = bits.length;
        }

        bits[idx] |= (1L << off);
    }


    public void reset() {
        for (int i = 0; i < len; i++) {
            bits[i] = 0;
        }
    }
}
