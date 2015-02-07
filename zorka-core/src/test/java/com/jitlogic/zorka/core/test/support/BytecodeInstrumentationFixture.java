/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.core.test.spy.support.TestSpyTransformer;
import com.jitlogic.zorka.core.test.spy.support.TestSubmitter;
import com.jitlogic.zorka.core.test.spy.support.TestTraceBuilder;
import org.junit.After;
import org.junit.Before;
import org.objectweb.asm.ClassWriter;

public class BytecodeInstrumentationFixture extends ZorkaFixture {

    public final static String MCLASS0 = "com.jitlogic.zorka.core.test.**";
    public final static String TCLASS1 = "com.jitlogic.zorka.core.test.spy.support.TestClass1";
    public final static String MCLASS1 = "com.jitlogic.zorka.core.test.spy.support.*Class1";
    public final static String TCLASS2 = "com.jitlogic.zorka.core.test.spy.support.TestClass2";
    public final static String TCLASS3 = "com.jitlogic.zorka.core.test.spy.support.TestClass3";
    public final static String TCLASS4 = "com.jitlogic.zorka.core.test.spy.support.TestClass4";

    public final static String ICLASS1 = "com.jitlogic.zorka.core.test.spy.support.TestInterface1";
    public final static String ICLASS2 = "com.jitlogic.zorka.core.test.spy.support.TestInterface2";

    public final static String TACLASS = "com.jitlogic.zorka.core.test.spy.support.ClassAnnotation";
    public final static String TAMETHOD = "com.jitlogic.zorka.core.test.spy.support.TestAnnotation";

    public TestSpyTransformer engine;
    public SymbolRegistry symbols;
    public TestSubmitter submitter;
    public TestTraceBuilder traceBuilder;
    public Tracer tracerObj;

    @Before
    public void setUp() throws Exception {
        engine = new TestSpyTransformer(agentInstance.getSymbolRegistry(),
                agentInstance.getTracer(), agentInstance.getRetransformer());
        submitter = new TestSubmitter();
        MainSubmitter.setSubmitter(submitter);
        traceBuilder = new TestTraceBuilder();
        tracerObj = new Tracer(agentInstance.getTracerMatcherSet(),
                agentInstance.getSymbolRegistry()) {
            public TraceBuilder getHandler() {
                return traceBuilder;
            }
        };
        MainSubmitter.setTracer(tracerObj);
        symbols = agentInstance.getSymbolRegistry();
    }

    @After
    public void tearDown() throws Exception {
        MainSubmitter.setSubmitter(null);
    }
}