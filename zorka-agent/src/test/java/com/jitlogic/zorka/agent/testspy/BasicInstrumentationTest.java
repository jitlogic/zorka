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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.spy.InstrumentationEngine;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.vmsci.MainSubmitter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BasicInstrumentationTest {

    private final static String TCLASS1 = "com.jitlogic.zorka.agent.testspy.TestClass1";

    private InstrumentationEngine engine;
    private TestSubmitter submitter;

    @Before
    public void setUp() throws Exception {
        engine = new InstrumentationEngine();
        submitter = new TestSubmitter();
        MainSubmitter.setSubmitter(submitter);
    }

    @After
    public void tearDown() throws Exception {
        MainSubmitter.setSubmitter(null);
    }

    @Test
    public void testClassWithoutAnyTransform() throws Exception{
        Object obj = TestUtil.instantiate(engine, TCLASS1);
        assertNotNull(obj);
        assertEquals(TCLASS1, obj.getClass().getName());
    }

    @Test
    public void testTrivialInstrumentOnlyEntryPointPlusSomeArg() throws Exception {
        engine.add(SpyDefinition.newInstance().onEnter().withArguments(0).lookFor(TCLASS1, "trivialMethod"));
        Object obj = TestUtil.instantiate(engine, TCLASS1);
        TestUtil.invoke(obj, "trivialMethod");
        assertEquals(1, submitter.size());
    }


}
