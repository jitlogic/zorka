package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.core.spy.*;


import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Test;


import java.util.List;

import static org.junit.Assert.*;

public class SpyClassResolverUnitTest extends ZorkaFixture {

    private static final String RP = "com/jitlogic/zorka/core/test/spy/crv/";
    private static final String LP = RP.replace('/', '.');
    private static final String O = SpyClassResolver.OBJECT_CLAZZ;

    private MethodCallStatistics stats = new MethodCallStatistics();
    private SpyClassResolver cr = new SpyClassResolver(stats);
    private ClassLoader cl = getClass().getClassLoader();

    @Test
    public void testGetClassInfo() throws Exception {
        CachedClassInfo i1 = cr.getClassInfo(cl, LP+"A");

        assertNotNull(i1);
        assertTrue("Class A should NOT be loaded.", i1 instanceof CachedClassInfo);
        assertEquals(LP+"A", i1.getClassName());

        CachedClassInfo s1 = cr.getClassInfo(cl, i1.getSuperclassName());
        assertNotNull(s1);
        assertTrue(s1 instanceof CachedClassInfo);
        assertEquals("java.lang.Object", s1.getClassName());

        Class.forName(LP+"B");
        CachedClassInfo i2 = cr.getClassInfo(cl, LP+"A");
        assertTrue(i2 instanceof CachedClassInfo);
        assertEquals(LP+"A", i2.getClassName());
    }

    @Test
    public void testGetAllSuperclasses() throws Exception {
        List<CachedClassInfo> lc = cr.getAllSuperclasses(cl, LP+"E");
        assertEquals(4, lc.size());

        assertEquals("java.lang.Object", lc.get(0).getClassName());
        assertEquals(LP+"A", lc.get(1).getClassName());
        assertEquals(LP+"B", lc.get(2).getClassName());
        assertEquals(LP+"D", lc.get(3).getClassName());
    }

    private void test(String te, String t1, String t2) {
        te = te.length() > 1 ? te : LP+te;
        t1 = LP+t1; t2 = LP + t2;
        assertEquals(
            "Common superclas of " + t1 + " and " + t2 + " should be " + te,
            te, cr.getCommonSuperClass(cl, t1, t2));
    }

    @Test
    public void testGetCommonSuperclassObjObj() throws Exception {
        test("A", "B", "C");
        test("A", "B", "A");
        test(O, "B", "X");
    }

    @Test
    public void testGetCommonSuperclassIfcIfc() throws Exception {
        test(O, "J", "K");
    }

    @Test
    public void testGetCommonSuperclassObjIfc() throws Exception {
        test("I", "I", "E");
        test("I", "E", "I");
        test("I", "I", "D");
        test("I", "D", "I");
    }

    @Test
    public void testGetCommonSuperclassObjIfcIndirect() throws Exception {
        test("K", "Z", "K");
        test("J", "Z", "J");
        // TODO superinterface case (ie. common interface implemented by type1 and type2) - if and when needed
    }

    @Test
    public void testFindLoadedClass() {
        SpyClassLookup u = new SpyClassLookup();

        Class clazz = u.findLoadedClass(ClassLoader.getSystemClassLoader(), "java.lang.Object");

        assertNotNull(clazz);
    }

}
