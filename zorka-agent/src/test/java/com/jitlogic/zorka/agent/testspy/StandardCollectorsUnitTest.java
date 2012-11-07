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

import com.jitlogic.zorka.spy.SpyCollector;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyRecord;

import static com.jitlogic.zorka.spy.SpyLib.*;

import com.jitlogic.zorka.spy.collectors.CallingBshCollector;
import com.jitlogic.zorka.spy.collectors.CallingObjCollector;
import com.jitlogic.zorka.spy.collectors.GetterPresentingCollector;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.getAttr;


public class StandardCollectorsUnitTest extends ZorkaFixture {

    private List<Object> results = new ArrayList<Object>();

    public void result(Object result) {
        results.add(result);
    }

    protected SpyContext ctx;
    protected SpyDefinition sdef;
    protected SpyRecord record;
    protected MBeanServer testMbs;

    @Before
    public void setUp() {
        zorkaAgent.installModule("test", this);

        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        mBeanServerRegistry.register("test", testMbs, null);

        sdef = SpyDefinition.instance();
        ctx = new SpyContext(sdef, "some.Class", "someMethod", "()V", 1);

        record = new SpyRecord(ctx);
    }


    @Test
    public void testFixtureIsWorkingProperly() throws Exception {
        zorkaAgent.eval("test.result(10)");

        assertEquals(1, results.size());
        assertEquals(10, results.get(0));
    }


    @Test
    public void testCollectRecordToPlainJavaObj() throws Exception {
        SpyCollector col = new CallingObjCollector(this, "result");
        record.feed(ON_COLLECT, new Object[] {1L, 2L, "abc"});

        col.collect(record);

        assertEquals(1, results.size());
        assertEquals(record, results.get(0));
    }


    @Test
    public void testCollectRecordViaBshFuncManual() throws Exception {
        zorkaAgent.eval("collect(obj) { test.result(obj); }");
        SpyCollector col = (SpyCollector)zorkaAgent.eval(
                "(com.jitlogic.zorka.spy.SpyCollector)this");
        record.feed(ON_COLLECT, new Object[] {1L, 2L});

        col.collect(record);

        assertEquals(1, results.size());
        assertEquals(record, results.get(0));
    }


    @Test
    public void testCollectRecordViaBshFuncViaCallingBshCollector() throws Exception {
        zorkaAgent.eval("collect(obj) { test.result(obj); }");
        SpyCollector col = new CallingBshCollector("this");
        record.feed(ON_COLLECT, new Object[] {1L, 2L});

        col.collect(record);

        assertEquals(1, results.size());
        assertEquals(record, results.get(0));
    }


    @Test
    public void testCollectRecordViaBshFuncInEmbeddedNamespace() throws Exception {
        zorkaAgent.eval("__that() { collect (obj) { test.result(obj); } return this; } that = __that();");
        SpyCollector col = new CallingBshCollector("that");
        record.feed(ON_COLLECT, new Object[] {1L, 2L});

        col.collect(record);

        assertEquals(1, results.size());
        assertEquals(record, results.get(0));
    }


    @Test
    public void testDefineCollectorFirstAndBshNamespaceAfterThat() throws Exception {
        SpyCollector col = new CallingBshCollector("that");
        zorkaAgent.eval("__that() { collect (obj) { test.result(obj); } return this; } that = __that();");
        record.feed(ON_COLLECT, new Object[]{1L, 2L});

        col.collect(record);

        assertEquals(1, results.size());
        assertEquals(record, results.get(0));
    }


    @Test
    public void testPublishObjectViaGetterCollector() throws Exception {
        SpyCollector col = new GetterPresentingCollector("test", "test:name=TestObj", "testAttr", "meh", 2);
        record.feed(ON_COLLECT, new Object[] {1L, 2L, "oja!"});

        col.collect(record);

        Object obj = getAttr("test", "test:name=TestObj", "testAttr");
        assertEquals("oja!", obj);
    }


    @Test
    public void testPublishObjectViaGetterCollectorWithDispatch() throws Exception {
        SpyCollector col = new GetterPresentingCollector("test", "test:name=TestObj", "testAttr", "meh", 2, "length()");
        record.feed(ON_COLLECT, new Object[] {1L, 2L, "oja!"});

        col.collect(record);

        Object obj = getAttr("test", "test:name=TestObj", "testAttr");
        assertEquals(4, obj);
    }
}
