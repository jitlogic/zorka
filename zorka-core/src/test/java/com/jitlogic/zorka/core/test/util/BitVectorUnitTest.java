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

package com.jitlogic.zorka.core.test.util;

import com.jitlogic.zorka.core.util.BitVector;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class BitVectorUnitTest {

    @Test
    public void testGetSetGetAllBitsSequential() {
        BitVector v = new BitVector(1);

        for (int i = 0; i < 1024; i++) {
            Assert.assertFalse("Expected FALSE on " + i + " iteration.", v.get(i));
            v.set(i);
            Assert.assertTrue("Expected TRUE on " + i + " iteration.", v.get(i));
        }
    }

    @Test
    public void testGetSetGetBitsRandom() {
        BitVector v = new BitVector(1);
        Random random = new Random();
        Set<Integer> refSet = new HashSet<Integer>();

        for (int i = 0; i < 65536; i++) {
            int n = random.nextInt(1024);
            if (refSet.contains(n)) {
                Assert.assertTrue("Expected TRUE on " + n + " slot.", v.get(n));
            } else {
                Assert.assertFalse("Expected FALSE on " + n + " slot.", v.get(n));
                v.set(n); refSet.add(n);
                Assert.assertTrue("Expected TRUE on " + n + " slot.", v.get(n));
            }
        }
    }

}
