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
package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import com.jitlogic.zorka.api.SpyLib;
import com.jitlogic.zorka.spy.*;

import com.jitlogic.zorka.spy.processors.GetterPresentingCollector;
import com.jitlogic.zorka.spy.processors.SpyProcessor;
import com.jitlogic.zorka.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.getAttr;


public class StandardCollectorsUnitTest extends ZorkaFixture {

    private List<Object> results = new ArrayList<Object>();

    public void result(Object result) {
        results.add(result);
    }

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected Map<String,Object> record;
    protected MBeanServer testMbs;

    @Before
    public void setUp() {
        zorkaAgent.install("test", this);

        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        mBeanServerRegistry.register("test", testMbs, null);

        sdef = SpyDefinition.instance();
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
        SpyProcessor col = (SpyProcessor)zorkaAgent.eval(
                "(com.jitlogic.zorka.spy.processors.SpyProcessor)this");

        col.process(record);

        assertEquals("should submit one result", 1, results.size());
        assertEquals("result should be Spy record", record, results.get(0));
    }


    @Test
    public void testPublishObjectViaGetterCollector() throws Exception {
        SpyProcessor col = new GetterPresentingCollector("test", "test:name=TestObj", "testAttr", "meh", "C2");
        record.put("C0", 1L); record.put("C1", 1L); record.put("C2", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_SUBMIT));
        record.put(".STAGE", SpyLib.ON_SUBMIT);

        col.process(record);

        Object obj = getAttr("test", "test:name=TestObj", "testAttr");
        assertEquals("getter should return string passed via spy record", "oja!", obj);
    }


    @Test
    public void testPublishObjectViaGetterCollectorWithDispatch() throws Exception {
        SpyProcessor col = new GetterPresentingCollector("test", "test:name=TestObj", "testAttr", "meh", "C2", "length()");
        record.put("C0", 1L); record.put("C1", 1L); record.put("C2", "oja!");

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << SpyLib.ON_SUBMIT));
        record.put(".STAGE", SpyLib.ON_SUBMIT);
        col.process(record);

        Object obj = getAttr("test", "test:name=TestObj", "testAttr");
        assertEquals("getter should return length of string passed via spy record", 4, obj);
    }
}
