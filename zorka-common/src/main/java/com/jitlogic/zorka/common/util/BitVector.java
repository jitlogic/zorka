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

package com.jitlogic.zorka.common.util;

/**
 * Maintains bit vector. Supplies get() and set() methods to get and set
 * individual bits in vector. Vector will automatically grow if client code
 * tries to set bits past the end of vector.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class BitVector {

    /** Vector data */
    private long bits[];

    /** Current vector length */
    private int len;

    /** Determines how much new element should be added every time vector has to be extended. */
    private int delta;

    // TODO add vector length limit, so some buggy code won't overrun memory

    /** Creates new vector of default initial length */
    public BitVector() {
        this(512);
    }

    /**
     * Creates new bit vector of determined initial length
     *
     * @param initial initial length
     */
    public BitVector(int initial) {
        len = delta = initial;
        bits = new long[initial];
    }


    /**
     * Returns true if given bit is set.
     *
     * @param bit bit number
     *
     * @return true if bit is set, false otherwise
     */
    public boolean get(int bit) {
        int idx = bit >> 6, off = bit & 63;
        return idx < len ? 0 != (bits[idx] & (1L << off)) : false;
    }


    /**
     * Sets on a bit in vector.
     *
     * @param bit bit number
     */
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


    /**
     * Zeroes all bits in this vector.
     */
    public void reset() {
        for (int i = 0; i < len; i++) {
            bits[i] = 0;
        }
    }
}
