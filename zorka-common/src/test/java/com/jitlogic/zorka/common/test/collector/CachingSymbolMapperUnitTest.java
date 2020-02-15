package com.jitlogic.zorka.common.test.collector;

import com.jitlogic.zorka.common.collector.CachingSymbolMapper;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicMethod;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import org.junit.Test;

import static com.jitlogic.zorka.common.tracedata.SymbolicMethod.of;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class CachingSymbolMapperUnitTest {

    @Test
    public void testMapSymbolsCache() {
        SymbolRegistry sr = new SymbolRegistry();
        sr.putSymbol(1, "FOO");
        sr.putSymbol(2, "BAR");
        sr.putSymbol(3, "BAZ");

        CachingSymbolMapper sm = new CachingSymbolMapper(sr);
        Map<Integer,Integer> sm1 = sm.newSymbols(ZorkaUtil.<Integer,String>map(41, "FOO", 42, "BAR"));
        assertEquals(ZorkaUtil.<Integer,Integer>map(41,1,42,2), sm1);
        assertEquals(0, sm.getHits());
        assertEquals(2, sm.getMisses());

        Map<Integer,Integer> sm2 = sm.newSymbols(ZorkaUtil.<Integer,String>map(42, "BAR", 43, "BAZ"));
        assertEquals(ZorkaUtil.<Integer,Integer>map(42,2,43,3), sm2);
        assertEquals(1, sm.getHits());
        assertEquals(3, sm.getMisses());
    }

    @Test
    public void testMethodsCache() {
        SymbolRegistry sr = new SymbolRegistry();
        sr.putMethod(1,11,21,31);
        sr.putMethod(2,12,22,32);
        sr.putMethod(3,13,23,33);

        CachingSymbolMapper sm = new CachingSymbolMapper(sr);
        HashMap<Integer, SymbolicMethod> m1 = ZorkaUtil.<Integer, SymbolicMethod>map(41, of(11, 21, 31), 42, of(12, 22, 32));
        Map<Integer,Integer> sm1 = sm.newMethods(m1);
        assertEquals(ZorkaUtil.<Integer,Integer>map(41,1,42,2), sm1);
        assertEquals(0, sm.getHits());
        assertEquals(2, sm.getMisses());

        HashMap<Integer, SymbolicMethod> m2 = ZorkaUtil.<Integer, SymbolicMethod>map(42, of(12, 22, 32), 43, of(13, 23, 33));
        Map<Integer,Integer> sm2 = sm.newMethods(m2);
        assertEquals(ZorkaUtil.<Integer,Integer>map(42,2,43,3), sm2);
        assertEquals(1, sm.getHits());
        assertEquals(3, sm.getMisses());
    }

}
