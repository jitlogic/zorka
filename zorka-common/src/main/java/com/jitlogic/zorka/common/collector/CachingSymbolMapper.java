package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.tracedata.SymbolicMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CachingSymbolMapper implements SymbolMapper {

    private SymbolMapper mapper;

    private Map<Integer,Integer> symbolMap = new ConcurrentHashMap<Integer,Integer>();
    private Map<Integer,Integer> methodMap = new ConcurrentHashMap<Integer,Integer>();

    private AtomicLong hits = new AtomicLong(0);
    private AtomicLong misses = new AtomicLong(0);

    public CachingSymbolMapper(SymbolMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<Integer, Integer> newSymbols(Map<Integer, String> newSymbols) {
        Map<Integer,Integer> rslt = new HashMap<Integer,Integer>();
        Map<Integer,String> req = new HashMap<Integer, String>();

        for (Map.Entry<Integer,String> e : newSymbols.entrySet()) {
            Integer id = symbolMap.get(e.getKey());
            if (id != null) {
                rslt.put(e.getKey(), id);
            } else {
                req.put(e.getKey(), e.getValue());
            }
        }

        // Update statistics
        hits.addAndGet(newSymbols.size()-req.size());
        misses.addAndGet(req.size());

        // No race condition here as symbol mapping operation is idempotent
        if (req.size() > 0) {
            Map<Integer,Integer> ret = mapper.newSymbols(req);
            symbolMap.putAll(ret);
            rslt.putAll(ret);
        }

        return rslt;
    }

    @Override
    public Map<Integer, Integer> newMethods(Map<Integer,SymbolicMethod> newMethods) {
        Map<Integer,Integer> rslt = new HashMap<Integer,Integer>();
        Map<Integer,SymbolicMethod> req = new HashMap<Integer,SymbolicMethod>();

        for (Map.Entry<Integer,SymbolicMethod> e : newMethods.entrySet()) {
            Integer id = methodMap.get(e.getKey());
            if (id != null) {
                rslt.put(e.getKey(), id);
            } else {
                req.put(e.getKey(), e.getValue());
            }
        }

        // Update statistics
        hits.addAndGet(newMethods.size()-req.size());
        misses.addAndGet(req.size());

        // No race condition here as symbol mapping operation is idempotent
        if (req.size() > 0) {
            Map<Integer,Integer> ret = mapper.newMethods(req);
            methodMap.putAll(ret);
            rslt.putAll(ret);
        }

        return rslt;
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }
}
