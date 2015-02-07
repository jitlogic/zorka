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


import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import static com.jitlogic.zorka.core.test.support.TestUtil.getAttr;
import static com.jitlogic.zorka.core.test.support.TestUtil.instantiate;
import static com.jitlogic.zorka.core.test.support.TestUtil.invoke;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OnlineReconfUnitTest extends ZorkaFixture {

    @Test
    public void testConfigureInstantiateRecofigureRun() throws Exception {
        SpyDefinition sdef1 = spy.instrument("test")
                .include(spy.byMethod(BytecodeInstrumentationFixture.TCLASS1, "trivialMethod"))
                .onSubmit(spy.zorkaStats("test", "test:name=TestClass1", "stats", "xxx"));

        agentInstance.getClassTransformer().add(sdef1);

        Object obj = instantiate(agentInstance.getClassTransformer(), BytecodeInstrumentationFixture.TCLASS1);

        SpyDefinition sdef2 = spy.instrument("test")
                .include(spy.byMethod(BytecodeInstrumentationFixture.TCLASS1, "trivialMethod"))
                .onSubmit(spy.zorkaStats("test", "test:name=TestClass1", "stats", "yyy"));

        agentInstance.getClassTransformer().add(sdef2);

        invoke(obj, "trivialMethod");

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=TestClass1", "stats");
        assertNotNull(stats);
        assertEquals("old configuration should not be used:", 0, stats.getMethodCallStatistic("xxx").getCalls());
        assertEquals("new configuration hasn't been used:", 1, stats.getMethodCallStatistic("yyy").getCalls());
    }

    @Test
    public void testConfigureInstantiateWithIncompatibleFetchConfiguration() throws Exception {
        SpyDefinition sdef1 = spy.instrument("test")
                .include(spy.byMethod(BytecodeInstrumentationFixture.TCLASS1, "trivialMethod"))
                .onSubmit(spy.zorkaStats("test", "test:name=TestClass1", "stats", "xxx"));

        agentInstance.getClassTransformer().add(sdef1);

        Object obj = instantiate(agentInstance.getClassTransformer(), BytecodeInstrumentationFixture.TCLASS1);

        SpyDefinition sdef2 = spy.instrument("test")
                .onEnter(spy.fetchThread("THREAD"))
                .include(spy.byMethod(BytecodeInstrumentationFixture.TCLASS1, "trivialMethod"))
                .onSubmit(spy.zorkaStats("test", "test:name=TestClass1", "stats", "yyy"));

        agentInstance.getClassTransformer().add(sdef2);

        invoke(obj, "trivialMethod");

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=TestClass1", "stats");
        assertNotNull(stats);
        assertEquals("old configuration should be used:", 1, stats.getMethodCallStatistic("xxx").getCalls());
        assertEquals("new configuration should be skipped due to incompatibility:",
                0, stats.getMethodCallStatistic("yyy").getCalls());
    }

    @Test
    public void testCheckInstrumentationInterface() {
        Method isModifiable = null, retransformMethod = null;

        for (Method m : Instrumentation.class.getDeclaredMethods()) {
            if ("isModifiableClass".equals(m.getName())) {
                isModifiable = m;
            }
            if ("retransformClasses".equals(m.getName())) {
                retransformMethod = m;
            }
        }

        assertNotNull("isModifiableMethod should exist", isModifiable);
        assertNotNull("retransformMethod should exist", retransformMethod);

    }

}
