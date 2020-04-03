/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.test;


import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.crypto.dom.DOMCryptoContext;
import java.util.Random;

import static com.jitlogic.zorka.common.util.ZorkaUtil.ipv4;
import static org.junit.Assert.*;

// TODO move this test to zorka-common some day

public class ZorkaUtilUnitTest {

    public static class TestCallStatistic extends MethodCallStatistic {
        public TestCallStatistic(String name) {
            super(name);
        }
    }

    @Test
    public void testInstanceOf1() throws Exception {
        assertTrue("immediate implements",
                ZorkaUtil.instanceOf(MethodCallStatistic.class, "com.jitlogic.zorka.common.stats.ZorkaStat"));
    }

    @Test
    public void testInstanceOf2() throws Exception {
        assertTrue("subinterface implements",   // TODO find better example (as this one doesn't matter anymore)
                ZorkaUtil.instanceOf(MethodCallStatistic.class, "com.jitlogic.zorka.common.stats.ZorkaStat"));
    }

    @Test
    public void testInstanceOf3() throws Exception {
        assertTrue("superclass implements subinterface", // TODO find better example (as this one doesn't matter anymore)
                ZorkaUtil.instanceOf(TestCallStatistic.class, "com.jitlogic.zorka.common.stats.ZorkaStat"));
    }

    @Test
    public void testInstanceOf4() throws Exception {
        assertFalse("should not implement",
                ZorkaUtil.instanceOf(MethodCallStatistic.class, "com.jitlogic.zorka.common.stats.ValGetter"));
    }

    @Test
    public void testInstanceOf5() throws Exception {
        assertTrue("immediate implements",
                ZorkaUtil.instanceOf(MethodCallStatistic.class, "com.jitlogic.zorka.common.stats.MethodCallStatistic"));
    }

    @Test
    public void testHexUnhex() throws Exception {
        byte[] b1 = new byte[32];
        new Random().nextBytes(b1);
        String s = ZorkaUtil.hex(b1);
        byte[] b2 = ZorkaUtil.hex(s);
        assertArrayEquals(b1, b2);
    }

    @Test
    public void testGenRestoreCryptKey() {
        String k = ZorkaUtil.generateKey();
        byte[][] kv = ZorkaUtil.parseKey(k);
        assertTrue(k.startsWith(ZorkaUtil.hex(kv[0])));
        assertTrue(k.endsWith(ZorkaUtil.hex(kv[1])));
    }

    @Test
    public void testIpv4Normalization() throws Exception {
        assertEquals("127.0.0.1", ipv4("127.0.0.1", "1.2.3.4"));
        assertEquals("127.0.0.1", ipv4("/127.0.0.1", "1.2.3.4"));
        assertEquals("127.0.0.1", ipv4("foobar", "127.0.0.1"));
    }


}
