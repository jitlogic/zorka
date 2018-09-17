package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;
import com.jitlogic.zorka.core.test.support.CoreTestUtil;
import org.junit.Before;
import org.junit.Test;
import com.jitlogic.zorka.core.spy.tuner.AbstractZtxReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ZtxFileProcessingUnitTest {

    private final List<String> lst = new ArrayList<String>();
    private final File ztxLog = new File("/tmp/_log.ztx");
    private final SymbolRegistry registry = new SymbolRegistry();

    @Before
    public void setUp() {
        ztxLog.delete();
    }

    @Test
    public void ztxFileReadUnitTest() throws Exception {
        final List<String[]> rslt = new ArrayList<String[]>();
        AbstractZtxReader er = new AbstractZtxReader() {
            @Override
            public void add(String p, String c, String m, String s) {
                rslt.add(new String[]{p, c, m, s});
            }
        };
        er.read(this.getClass().getResourceAsStream("/tuner/test.ztx"));
        assertEquals(6, rslt.size());
    }

    @Test
    public void ztxMatcherSetInitUnitTest() throws Exception {
        ZtxMatcherSet zms = new ZtxMatcherSet(
                new File("src/test/resources/tuner"),
                ztxLog, registry, true);

        List<String> pkgs = CoreTestUtil.getField(zms, "ztxPkgs");
        assertTrue(pkgs.size() > 2);
        assertTrue(pkgs.contains("test"));
    }

    @Test
    public void ztxMatcherSetLoadUnitTest() throws Exception {
        ZtxMatcherSet zms = new ZtxMatcherSet(
                new File("src/test/resources/tuner"),
                ztxLog, registry, true);

        assertFalse("Class is present in exclusions", zms.classMatch("test.myapp.SomeClass"));
        assertTrue("Class is absent in exclusions", zms.classMatch("test.myapp.SomeClass1"));
    }

    @Test
    public void ztxMatcherSetAddDefUnitTest() {
        ZtxMatcherSet zms = new ZtxMatcherSet(
                new File("src/test/resources/tuner"),
                ztxLog, registry, true);
        assertTrue(zms.methodMatch("test.myapp.SomeClass1", null, null, null, 1, "someMethod", "()V", null));
        zms.add("test.myapp.SomeClass1", "someMethod", "()V");
        assertFalse(zms.methodMatch("test.myapp.SomeClass1", null, null, null, 1, "someMethod", "()V", null));
        assertTrue(ztxLog.length() > 0);
    }
}
