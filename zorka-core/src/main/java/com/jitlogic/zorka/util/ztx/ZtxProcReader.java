package com.jitlogic.zorka.util.ztx;

import com.jitlogic.zorka.core.spy.tuner.AbstractZtxReader;

import java.util.*;

public class ZtxProcReader extends AbstractZtxReader {

    private NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data;

    public ZtxProcReader(NavigableMap<String,NavigableMap<String,NavigableMap<String,NavigableSet<String>>>> data) {
        this.data = data;
    }

    @Override
    public void add(String p, String c, String m, String s) {
        NavigableMap<String,NavigableMap<String,NavigableSet<String>>> mp = data.get(p);
        if (mp == null) {
            mp = new TreeMap<String,NavigableMap<String,NavigableSet<String>>>();
            data.put(p,mp);
        }

        NavigableMap<String,NavigableSet<String>> mc = mp.get(c);
        if (mc == null) {
            mc = new TreeMap<String,NavigableSet<String>>();
            mp.put(m, mc);
        }

        NavigableSet<String> ms = mc.get(m);
        if (ms == null) {
            ms = new TreeSet<String>();
            mc.put(m, ms);
        }

        ms.add(s);
    }
}
