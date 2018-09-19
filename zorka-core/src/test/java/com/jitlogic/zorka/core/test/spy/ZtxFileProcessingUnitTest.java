package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;
import com.jitlogic.zorka.core.test.support.CoreTestUtil;
import com.jitlogic.zorka.util.ZorkaUtilMain;
import com.jitlogic.zorka.util.ztx.ZtxProcReader;
import org.junit.Before;
import org.junit.Test;
import com.jitlogic.zorka.core.spy.tuner.AbstractZtxReader;

import java.io.File;
import java.util.*;

import static com.jitlogic.zorka.common.util.ZorkaUtil.rmrf;
import static org.junit.Assert.*;

public class ZtxFileProcessingUnitTest {

    private List<String> lst = new ArrayList<String>();
    private File tmpDir;
    private File ztxLog;
    private SymbolRegistry registry = new SymbolRegistry();

    @Before
    public void setUp() throws Exception {
        tmpDir = new File("/tmp/zorka-unit-test");
        rmrf(tmpDir);
        tmpDir.mkdirs();
        ztxLog = new File(tmpDir, "_log.ztx");

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

        Map<String,String> pkgs = CoreTestUtil.getField(zms, "ztxs");
        assertTrue(pkgs.size() > 2);
        assertTrue(pkgs.containsKey("test"));
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

    private void check(NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data,
            String pkg, String cls, String met, String sig) {
        assertNotNull("Package not found: " + pkg, data.get(pkg));
        assertNotNull("Class not found: " + cls, data.get(pkg).get(cls));
        assertNotNull("Method not found: " + met, data.get(pkg).get(cls).get(met));
        assertTrue("Signature not found: " + sig, data.get(pkg).get(cls).get(met).contains(sig));
    }

    @Test
    public void ztxProcReaderTest() throws Exception {
        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> idata
                = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        new ZtxProcReader(idata).read("src/test/resources/tuner/test2.ztx");

        check(idata,"test.myapp", "SomeClass", "otherMethod", "()V");
    }

    @Test
    public void ztxToolMergeTwoFilesNoFiltering() throws Exception {
        File outf = new File(tmpDir, "out.ztx");
        String[] args = {
                "ztx", "-o", outf.getPath(),
                "-f", "src/test/resources/tuner/test.ztx",
                "-f", "src/test/resources/tuner/test2.ztx"
        };
        ZorkaUtilMain.main(args);

        assertTrue(outf.isFile());
        assertTrue(outf.length() > 0);

        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> idata
                = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        new ZtxProcReader(idata).read(outf);

        check(idata, "test.myapp", "SomeClass", "myMethod", "()V");
        check(idata, "test.myapp", "SomeClass", "otherMethod", "()V");

        assertNotNull(idata.get("test.otherapp"));
    }

    @Test
    public void ztxToolMergeWithFilterIncl() throws Exception {
        File outf = new File(tmpDir, "out.ztx");
        String[] args = {
                "ztx", "-o", outf.getPath(),
                "-f", "src/test/resources/tuner/test.ztx",
                "-f", "src/test/resources/tuner/test2.ztx",
                "-i", "test.myapp"
        };
        ZorkaUtilMain.main(args);

        assertTrue(outf.isFile());

        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> idata
                = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        new ZtxProcReader(idata).read(outf);

        assertNotNull(idata.get("test.myapp"));
        assertEquals(1, idata.size());
    }

    @Test
    public void ztxToolMergeWithFilterExcl() throws Exception {
        File outf = new File(tmpDir, "out.ztx");
        String[] args = {
                "ztx", "-o", outf.getPath(),
                "-f", "src/test/resources/tuner/test.ztx",
                "-f", "src/test/resources/tuner/test2.ztx",
                "-x", "test.myapp"
        };
        ZorkaUtilMain.main(args);

        assertTrue(outf.isFile());

        NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> idata
                = new TreeMap<String, NavigableMap<String, NavigableMap<String, NavigableSet<String>>>>();

        new ZtxProcReader(idata).read(outf);

        assertNull(idata.get("test.myapp"));
        assertNotNull(idata.get("test.otherapp"));
        assertEquals(2, idata.size());

    }
}
