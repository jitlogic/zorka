package com.jitlogic.zorka.core.test.integ;

import com.jitlogic.zorka.core.integ.zabbix.ZabbixQueryTranslator;
import org.junit.Test;


import static org.junit.Assert.*;

public class ZabbixQueryTranslationUnitTest {

    @Test
    public void testVarsAndAttrs() {
        ZabbixQueryTranslator t = new ZabbixQueryTranslator(null);
        assertEquals("var", t.translate("var"));
        assertEquals("some.var", t.translate("some__var"));
    }

    @Test
    public void testFuncs() {
        ZabbixQueryTranslator t = new ZabbixQueryTranslator(null);
        assertEquals("zorka.version()", t.translate("zorka__version[]"));
        assertEquals("some_func(1,2,3)", t.translate("some_func[1,2,3]"));
        assertEquals("func2(\"abc\")", t.translate("func2[\"abc\"]"));
        assertEquals("func3(\"ab[]cd\")", t.translate("func3[\"ab[]cd\"]"));
    }

    @Test
    public void testBiggerFuncs() {
        ZabbixQueryTranslator t = new ZabbixQueryTranslator(null);
        assertEquals("zorka.jmx(\"java\", \"java.lang:type=OperatingSystem\", \"Arch\")",
                t.translate("zorka__jmx[\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]"));
    }

    @Test
    public void testPrefixedFuncs() {
        ZabbixQueryTranslator t = new ZabbixQueryTranslator("test");
        assertEquals("zabbix.advertise()", t.translate("zabbix.advertise[*]"));
        assertNull(t.translate("zorka.version()"));
        assertEquals("zorka.version()", t.translate("zorka.version[test]"));
        assertNull(t.translate("zorka.version[test2]"));
        assertEquals("some_func(1,2,3)", t.translate("some_func[test,1,2,3]"));
        assertEquals("zorka.jmx(\"java\", \"java.lang:type=OperatingSystem\", \"Arch\")",
                t.translate("zorka__jmx[\"test\" ,\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]"));
    }
}
