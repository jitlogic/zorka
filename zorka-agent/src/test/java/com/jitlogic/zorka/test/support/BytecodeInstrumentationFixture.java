package com.jitlogic.zorka.test.support;

import com.jitlogic.zorka.spy.MainSubmitter;
import com.jitlogic.zorka.test.spy.support.TestSpyTransformer;
import com.jitlogic.zorka.test.spy.support.TestSubmitter;
import com.jitlogic.zorka.test.spy.support.TestTracer;
import com.jitlogic.zorka.tracer.SymbolRegistry;
import org.junit.After;
import org.junit.Before;

public class BytecodeInstrumentationFixture extends ZorkaFixture {

    public final static String TCLASS1 = "com.jitlogic.zorka.test.spy.support.TestClass1";
    public final static String TCLASS2 = "com.jitlogic.zorka.test.spy.support.TestClass2";
    public final static String TACLASS = "com.jitlogic.zorka.test.spy.support.ClassAnnotation";
    public final static String TAMETHOD = "com.jitlogic.zorka.test.spy.support.TestAnnotation";

    public TestSpyTransformer engine;
    public SymbolRegistry symbols;
    public TestSubmitter submitter;
    public TestTracer tracer;

    @Before
    public void setUp() throws Exception {
        engine = new TestSpyTransformer();
        submitter = new TestSubmitter();
        MainSubmitter.setSubmitter(submitter);
        tracer = new TestTracer();
        MainSubmitter.setTracer(tracer);
        symbols = engine.getSymbolRegistry();
    }

    @After
    public void tearDown() throws Exception {
        MainSubmitter.setSubmitter(null);
    }
}