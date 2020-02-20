/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package org.slf4j.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class ZorkaLoggerUnitTest {

    private Properties props(String...props) {
         Properties rslt = new Properties();
         for (int i = 1; i < props.length; i+=2) {
             rslt.put(props[i-1],props[i]);
         }
         return rslt;
    }

    @Before
    public void resetLogger() {
        ZorkaLoggerFactory.getInstance().shutdown();
    }

    @Test
    public void testParseAndConfigureLogLevels() {
        ZorkaLoggerFactory lf = ZorkaLoggerFactory.getInstance();

        ZorkaTrapperLogger l1 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.SomeClass");
        ZorkaTrapperLogger l2 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.OtherClass");
        ZorkaTrapperLogger l3 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.Some");

        Assert.assertEquals(20, l1.getLogLevel());
        Assert.assertEquals(20, l2.getLogLevel());
        Assert.assertEquals(20, l3.getLogLevel());

        lf.configure(props(
                "log", "ERROR",
                "log.com.myapp", "WARN",
                "log.com.myapp.buggy", "DEBUG",
                "log.com.myapp.buggy.Some", "TRACE"));

        Assert.assertEquals(30, l1.getLogLevel());
        Assert.assertEquals(10, l2.getLogLevel());
        Assert.assertEquals(0, l3.getLogLevel());

        List<ZorkaLoggerFactory.LogLevel> ll = lf.getLogLevels();

        Assert.assertEquals(0, ll.get(0).getLevel());
        Assert.assertEquals("com.myapp.buggy.Some", ll.get(0).getPrefix());

        Assert.assertEquals(10, ll.get(1).getLevel());
        Assert.assertEquals("com.myapp.buggy", ll.get(1).getPrefix());

        Assert.assertEquals(30, ll.get(2).getLevel());
        Assert.assertEquals("com.myapp", ll.get(2).getPrefix());

        ZorkaTrapperLogger l4 = (ZorkaTrapperLogger)lf.getLogger("com.jitlogic.YetAnotherClass");
        Assert.assertEquals(40, l4.getLogLevel());
    }

    @Test
    public void testLogginAtVariousLogLevels() {

        ZorkaLoggerFactory lf = ZorkaLoggerFactory.getInstance();

        ZorkaTrapperLogger l1 = (ZorkaTrapperLogger)lf.getLogger("org.myapp.SomeClass");
        ZorkaTrapperLogger l2 = (ZorkaTrapperLogger)lf.getLogger("org.myapp.buggy.OtherClass");
        ZorkaTrapperLogger l3 = (ZorkaTrapperLogger)lf.getLogger("org.myapp.buggy.Some");
        ZorkaTrapperLogger l4 = (ZorkaTrapperLogger)lf.getLogger("org.jitlogic.YetAnotherClass");

        lf.configure(props(
                "log", "ERROR",
                "log.org.myapp", "WARN",
                "log.org.myapp.buggy", "DEBUG",
                "log.org.myapp.buggy.Some", "TRACE"));

        MemoryTrapper mt = (MemoryTrapper)lf.getTrapper();

        l1.trace("OJA!");
        Assert.assertEquals(0, mt.drain().size());
        l1.info("OJA!");
        Assert.assertEquals(0, mt.drain().size());
        l1.warn("OJA!");
        Assert.assertEquals(1, mt.drain().size());
        l1.error("OJA!");
        Assert.assertEquals(1, mt.drain().size());

        l3.trace("OJA!");
        Assert.assertEquals(1, mt.drain().size());
        l3.error("OJA!");
        Assert.assertEquals(1, mt.drain().size());

        l4.warn("OJA!");
        Assert.assertEquals(0, mt.drain().size());
        l4.error("OJA!");
        Assert.assertEquals(1, mt.drain().size());
    }

    @Test
    public void testFormatSlfArgs() {
        Assert.assertEquals("foo", ZorkaTrapperLogger.format("foo"));
        Assert.assertEquals("a1b2c3d", ZorkaTrapperLogger.format("a{}b{}c{}d", 1, 2, 3));
        Assert.assertEquals("a1b2cd", ZorkaTrapperLogger.format("a{}b{}c{}d", 1, 2));
    }

}
