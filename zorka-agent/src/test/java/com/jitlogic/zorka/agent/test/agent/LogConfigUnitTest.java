/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.test.agent;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;
import com.jitlogic.zorka.common.ZorkaLogConfig;

import com.jitlogic.zorka.common.ZorkaLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import  static org.junit.Assert.*;

public class LogConfigUnitTest extends ZorkaFixture {

    @Before
    public void setTestProps() {
        ZorkaConfig.getProperties().setProperty("test.int", " 10 ");
        ZorkaConfig.getProperties().setProperty("test.broken", "oja!");
        ZorkaConfig.getProperties().setProperty("test.space", "  ");
        ZorkaConfig.getProperties().setProperty("test.lst", "  oja!,  oje! ");
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

}
