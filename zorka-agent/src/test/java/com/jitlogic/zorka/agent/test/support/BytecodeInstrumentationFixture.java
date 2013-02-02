/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent.test.support;

import com.jitlogic.zorka.common.SymbolRegistry;
import com.jitlogic.zorka.common.TraceBuilder;
import com.jitlogic.zorka.agent.spy.*;
import com.jitlogic.zorka.agent.test.spy.support.TestSpyTransformer;
import com.jitlogic.zorka.agent.test.spy.support.TestSubmitter;
import com.jitlogic.zorka.agent.test.spy.support.TestTracer;
import org.junit.After;
import org.junit.Before;

public class BytecodeInstrumentationFixture extends ZorkaFixture {

    public final static String TCLASS1 = "com.jitlogic.zorka.agent.test.spy.support.TestClass1";
    public final static String TCLASS2 = "com.jitlogic.zorka.agent.test.spy.support.TestClass2";
    public final static String TCLASS3 = "com.jitlogic.zorka.agent.test.spy.support.TestClass3";

    public final static String TACLASS = "com.jitlogic.zorka.agent.test.spy.support.ClassAnnotation";
    public final static String TAMETHOD = "com.jitlogic.zorka.agent.test.spy.support.TestAnnotation";

    public TestSpyTransformer engine;
    public SymbolRegistry symbols;
    public TestSubmitter submitter;
    public TestTracer output;
    public Tracer t;

    @Before
    public void setUp() throws Exception {
        engine = new TestSpyTransformer(spyInstance.getTracer());
        submitter = new TestSubmitter();
        MainSubmitter.setSubmitter(submitter);
        output = new TestTracer();
        t = new Tracer() {
            public TraceBuilder getHandler() {
                return output;
            }
        };
        MainSubmitter.setTracer(t);
        symbols = engine.getSymbolRegistry();
    }

    @After
    public void tearDown() throws Exception {
        MainSubmitter.setSubmitter(null);
    }
}