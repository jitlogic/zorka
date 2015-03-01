/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.*;

import com.jitlogic.zorka.core.spy.plugins.GetterPresentingCollector;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.test.support.TestUtil.getAttr;


public class StandardCollectorsUnitTest extends ZorkaFixture {

    private List<Object> results = new ArrayList<Object>();

    // Don't remove. This is used from test BSH scripts.
    public void result(Object result) {
        results.add(result);
    }

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected Map<String, Object> record;

    @Before
    public void setUp() {
        zorkaAgent.put("test", this);

        sdef = spy.instance("x");
        ctx = new SpyContext(sdef, "some.Class", "someMethod", "()V", 1);

        record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
    }


    @Test
    public void testFixtureIsWorkingProperly() throws Exception {
        zorkaAgent.eval("test.result(10)");

        assertEquals("should find one result", 1, results.size());
        assertEquals("result should be an integer", 10, results.get(0));
    }


    @Test
    public void testCollectRecordViaBshFuncManual() throws Exception {
        zorkaAgent.eval("process(obj) { test.result(obj); }");
        SpyProcessor col = (SpyProcessor) zorkaAgent.eval(
                "(com.jitlogic.zorka.core.spy.SpyProcessor)this");

        col.process(record);

        assertEquals("should submit one result", 1, results.size());
        assertEquals("result should be Spy record", record, results.get(0));
    }


    @Test
    public void testPublishObjectViaGetterCollector() throws Exception {
        SpyProcessor col = new GetterPresentingCollector(mBeanServerRegistry, "test", "test:name=TestObj", "testAttr", "meh", "C2");
        record.put("C0", 1L);
        record.put("C1", 1L);
        record.put("C2", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_SUBMIT));
        record.put(".STAGE", SpyLib.ON_SUBMIT);

        col.process(record);

        Object obj = getAttr(testMbs, "test:name=TestObj", "testAttr");
        assertEquals("getter should return string passed via spy record", "oja!", obj);
    }


    @Test
    public void testPublishObjectViaGetterCollectorWithDispatch() throws Exception {
        SpyProcessor col = new GetterPresentingCollector(mBeanServerRegistry, "test", "test:name=TestObj", "testAttr", "meh", "C2", "length()");
        record.put("C0", 1L);
        record.put("C1", 1L);
        record.put("C2", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_SUBMIT));
        record.put(".STAGE", SpyLib.ON_SUBMIT);
        col.process(record);

        Object obj = getAttr(testMbs, "test:name=TestObj", "testAttr");
        assertEquals("getter should return length of string passed via spy record", 4, obj);
    }
}
