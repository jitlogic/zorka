package com.jitlogic.zorka.core.spy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpyStateShelf {

    private static Logger log = LoggerFactory.getLogger(SpyStateShelf.class);

    private Map<Object,SpyStateShelfData> objects = new ConcurrentHashMap<Object, SpyStateShelfData>();

    public synchronized void shelve(Object key, SpyStateShelfData obj) {
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Shelving: " + key + " obj=" + obj);
        }
        objects.put(key, obj);
    }

    public synchronized SpyStateShelfData unshelve(Object key) {
        SpyStateShelfData obj = objects.get(key);
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Unshelving: " + key + ", obj=" + obj);
        }
        if (obj != null) objects.remove(key);
        return obj;
    }

    public synchronized int cleanup() {
        int rslt = 0;
        long t = System.currentTimeMillis();
        for (Map.Entry<Object,SpyStateShelfData> e : objects.entrySet()) {
            if (e.getValue().getTimeLimit() < t) {
                rslt++;
                objects.remove(e.getKey());
            }
        }
        return rslt;
    }

}
