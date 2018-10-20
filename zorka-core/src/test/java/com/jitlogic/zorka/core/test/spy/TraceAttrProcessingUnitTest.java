/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.test.spy;


import com.jitlogic.zorka.common.tracedata.TaggedValue;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.plugins.SpyFlagsProcessor;
import com.jitlogic.zorka.core.spy.plugins.TraceAttrProcessor;
import com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;



public class TraceAttrProcessingUnitTest extends BytecodeInstrumentationFixture {

    @Test
    public void testTraceUntaggedAttr() {

        new TraceAttrProcessor(symbols, tracerObj, TraceAttrProcessor.FIELD_GETTING_PROCESSOR, "SQL", null, "SQL", null).process(
                ZorkaUtil.<String, Object>map("SQL", "select * from table"));

        traceBuilder.check(0, "action", "newAttr", "attrId", symbols.symbolId("SQL"));
        traceBuilder.check(0, "attrVal", "select * from table");
    }


    @Test
    public void testTraceFormattedAttr() {
        new TraceAttrProcessor(symbols, tracerObj, TraceAttrProcessor.STRING_FORMAT_PROCESSOR, "${SQL} GO", null, "SQL", null).process(
                ZorkaUtil.<String, Object>map("SQL", "select 1"));

        traceBuilder.check(0, "action", "newAttr", "attrId", symbols.symbolId("SQL"));
        traceBuilder.check(0, "attrVal", "select 1 GO");
    }


    @Test
    public void testTraceTaggedAttr() {
        new TraceAttrProcessor(symbols, tracerObj, TraceAttrProcessor.FIELD_GETTING_PROCESSOR, "SQL", null, "SQL", "SQL_QUERY").process(
                ZorkaUtil.<String, Object>map("SQL", "select * from table"));

        traceBuilder.check(0, "action", "newAttr", "attrId", symbols.symbolId("SQL"));
        traceBuilder.check(0, "attrVal", new TaggedValue(symbols.symbolId("SQL_QUERY"), "select * from table"));
    }

    @Test
    public void testTraceSetErrorFlags() {
        SpyProcessor sp1 = new SpyFlagsProcessor(true), sp2 = new SpyFlagsProcessor(false);

        Map<String,Object> m = sp1.process(ZorkaUtil.<String, Object>map(".STAGES", 0));

        assertEquals(1 << SpyLib.ON_ERROR, m.get(".STAGES"));

        m = sp2.process(m);
        assertEquals(1 << SpyLib.ON_RETURN, m.get(".STAGES"));

    }

}
