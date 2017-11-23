/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.agent;

import com.jitlogic.zorka.common.test.support.TestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import com.jitlogic.zorka.common.util.ZorkaLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import  static org.junit.Assert.*;

public class AgentConfigUnitTest extends ZorkaFixture {

    @Before
    public void setTestProps() {
        config.getProperties().setProperty("test.int", " 10 ");
        config.getProperties().setProperty("test.broken", "oja!");
        config.getProperties().setProperty("test.space", "  ");
        config.getProperties().setProperty("test.lst", "  oja!,  oje! ");
        config.getProperties().setProperty("ten.bytes", "10");
        config.getProperties().setProperty("one.kilobyte", "1k");
        config.getProperties().setProperty("ten.megabytes", "10M");

        Map<String,String> m = new HashMap();
        m.putAll((Map<String,String>)util.getField(config, "sysenv"));
        m.put("FOO", "BAR");
        util.setField(config, "sysenv", m);
    }


    @Test
    public void testParseSimpleLogConfigStrings() {
        assertEquals(ZorkaLogger.ZTR_CONFIG, ZorkaLogger.parse("", "ZTR", "CONFIG"));
        assertEquals(ZorkaLogger.ZTR_CONFIG | ZorkaLogger.ZTR_TRACE_CALLS,
                ZorkaLogger.parse("", "ZTR", "CONFIG,TRACE_CALLS"));
        assertEquals(ZorkaLogger.ZTR_CONFIG | ZorkaLogger.ZTR_TRACE_CALLS,
                ZorkaLogger.parse("", "ZTR", "config, trace_calls"));
    }


    @Test
    public void testParseIntPropsViaZorkaLib() {
        assertEquals((Integer) 10, zorka.intCfg("test.int"));
        assertEquals((Integer)10, zorka.intCfg("test.int", 30));
        assertEquals(null, zorka.intCfg("test.broken"));
        assertEquals((Integer)20, zorka.intCfg("test.broken", 20));
        assertEquals(null, zorka.intCfg("test.missing"));
    }


    @Test
    public void testParseLongPropsViaZorkaLib() {
        assertEquals((Long)10L, zorka.longCfg("test.int"));
        assertEquals((Long)10L, zorka.longCfg("test.int", 30L));
        assertEquals(null, zorka.longCfg("test.broken"));
        assertEquals((Long)20L, zorka.longCfg("test.broken", 20L));
        assertEquals(null, zorka.longCfg("test.missing"));
    }


    @Test
    public void testParseStringPropsViaZorkaLib() {
        assertEquals(null, zorka.stringCfg("test.missing"));
        assertEquals("", zorka.stringCfg("test.space"));
        assertEquals("oja!", zorka.stringCfg("test.missing", "oja!"));
    }


    @Test
    public void testParseListPropsViaZorkaLib() {
        assertEquals(new ArrayList<String>(), zorka.listCfg("test.missing"));
        assertEquals(new ArrayList<String>(), zorka.listCfg("test.space"));
        assertEquals(Arrays.asList("oja!"), zorka.listCfg("test.broken"));
        assertEquals(Arrays.asList("oja!", "oje!"), zorka.listCfg("test.lst"));
    }

    @Test
    public void testParseKiloProps() {
        assertEquals((Long)10L, zorka.kiloCfg("ten.bytes"));
        assertEquals((Long)1024L, zorka.kiloCfg("one.kilobyte"));
        assertEquals((Long)10485760L, zorka.kiloCfg("ten.megabytes"));
    }

    @Test
    public void testParseEnvVar() {
        config.getProperties().put("test.foo", "${$FOO}");
        assertEquals("BAR", zorka.stringCfg("test.foo"));
    }

    @Test
    public void testParseEnvVarNonExistent() {
        config.getProperties().put("test.foo1", "${$FOO1}r");
        assertEquals("${$FOO1}r", zorka.stringCfg("test.foo1")); // TODO should this expand to null/empty ?
    }

    @Test
    public void testParseEnvVarNonExistentWithDefVal() {
        config.getProperties().put("test.foo1", "${$FOO1:ba}r");
        assertEquals("bar", zorka.stringCfg("test.foo1"));
    }

    @Test
    public void testParseSysProp() {
        config.getProperties().put("test.bar", "${@sys.bar}");
        System.setProperty("sys.bar", "foo");
        assertEquals("foo", zorka.stringCfg("test.bar"));
    }

    @Test
    public void testParseSysPropNonExistentWithDefVal() {
        config.getProperties().put("test.bar1", "${@sys.bar1:ooo}");
        assertEquals("ooo", zorka.stringCfg("test.bar1"));
    }

    @Test
    public void testParseCfgVal() {
        config.getProperties().put("test.foo", "bar");
        config.getProperties().put("test.bar", "${&test.foo}");
        assertEquals("bar", zorka.stringCfg("test.bar"));
    }
}
