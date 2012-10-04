package com.jitlogic.zorka.agent.unittest;

/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.util.ZorkaUtil;
import junit.framework.Assert;
import org.junit.Test;

public class ZorkaUtilTest {

    public static class TestCallStatistic extends MethodCallStatistic {
        public TestCallStatistic(String name) { super(name); }
    }

    @Test
    public void testInstanceOf1() throws Exception {
        Assert.assertTrue("immediate implements",
            ZorkaUtil.instanceOfIfc(MethodCallStatistic.class, "com.jitlogic.zorka.mbeans.MethodCallStat"));
    }

    @Test
    public void testInstanceOf2() throws Exception {
        Assert.assertTrue("subinterface implements",
                ZorkaUtil.instanceOfIfc(MethodCallStatistic.class, "com.jitlogic.zorka.mbeans.ZorkaStat"));
    }

    @Test
    public void testInstanceOf3() throws Exception {
        Assert.assertTrue("superclass implements subinterface",
                ZorkaUtil.instanceOfIfc(TestCallStatistic.class, "com.jitlogic.zorka.mbeans.MethodCallStat"));
    }

    @Test
    public void testInstanceOf4() throws Exception {
        Assert.assertFalse("should not implement",
                ZorkaUtil.instanceOfIfc(MethodCallStatistic.class, "com.jitlogic.zorka.mbeans.ValGetter"));
    }
}
