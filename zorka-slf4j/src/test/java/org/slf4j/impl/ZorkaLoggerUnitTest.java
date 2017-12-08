package org.slf4j.impl;

import org.junit.Test;
import static org.junit.Assert.*;

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

    @Test
    public void testParseAndConfigureLogLevels() {
        ZorkaLoggerFactory lf = ZorkaLoggerFactory.getInstance();

        ZorkaTrapperLogger l1 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.SomeClass");
        ZorkaTrapperLogger l2 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.OtherClass");
        ZorkaTrapperLogger l3 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.Some");

        assertEquals(20, l1.getLogLevel());
        assertEquals(20, l2.getLogLevel());
        assertEquals(20, l3.getLogLevel());

        lf.configure(props(
                "log", "ERROR",
                "log.com.myapp", "WARN",
                "log.com.myapp.buggy", "DEBUG",
                "log.com.myapp.buggy.Some", "TRACE"));

        assertEquals(30, l1.getLogLevel());
        assertEquals(10, l2.getLogLevel());
        assertEquals(0, l3.getLogLevel());

        List<ZorkaLoggerFactory.LogLevel> ll = lf.getLogLevels();

        assertEquals(0, ll.get(0).getLevel());
        assertEquals("com.myapp.buggy.Some", ll.get(0).getPrefix());

        assertEquals(10, ll.get(1).getLevel());
        assertEquals("com.myapp.buggy", ll.get(1).getPrefix());

        assertEquals(30, ll.get(2).getLevel());
        assertEquals("com.myapp", ll.get(2).getPrefix());

        ZorkaTrapperLogger l4 = (ZorkaTrapperLogger)lf.getLogger("com.jitlogic.YetAnotherClass");
        assertEquals(40, l4.getLogLevel());
    }

    @Test
    public void testLogginAtVariousLogLevels() {

        ZorkaLoggerFactory lf = ZorkaLoggerFactory.getInstance();

        ZorkaTrapperLogger l1 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.SomeClass");
        ZorkaTrapperLogger l2 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.OtherClass");
        ZorkaTrapperLogger l3 = (ZorkaTrapperLogger)lf.getLogger("com.myapp.buggy.Some");
        ZorkaTrapperLogger l4 = (ZorkaTrapperLogger)lf.getLogger("com.jitlogic.YetAnotherClass");

        lf.configure(props(
                "log", "ERROR",
                "log.com.myapp", "WARN",
                "log.com.myapp.buggy", "DEBUG",
                "log.com.myapp.buggy.Some", "TRACE"));

        MemoryTrapper mt = (MemoryTrapper)lf.getTrapper();

        l1.trace("OJA!");assertEquals(0, mt.drain().size());
        l1.info("OJA!");assertEquals(0, mt.drain().size());
        l1.warn("OJA!");assertEquals(1, mt.drain().size());
        l1.error("OJA!");assertEquals(1, mt.drain().size());

        l3.trace("OJA!");assertEquals(1, mt.drain().size());
        l3.error("OJA!");assertEquals(1, mt.drain().size());

        l4.warn("OJA!");assertEquals(0, mt.drain().size());
        l4.error("OJA!");assertEquals(1, mt.drain().size());
    }

}
