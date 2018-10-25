package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.LimitedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpyStateShelf<K, T extends LimitedTime> {

    private static Logger log = LoggerFactory.getLogger(SpyStateShelf.class);

    private Map<K,T> objects = new ConcurrentHashMap<K, T>();

    public synchronized void shelve(K key, T obj) {
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Shelving: " + key + " obj=" + obj);
        }
        objects.put(key, obj);
    }

    public synchronized T unshelve(K key) {
        T obj = objects.get(key);
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Unshelving: " + key + ", obj=" + obj);
        }
        if (obj != null) objects.remove(key);
        return obj;
    }

    public synchronized int cleanup() {
        int rslt = 0;
        long t = System.currentTimeMillis();
        for (Map.Entry<K,? extends LimitedTime> e : objects.entrySet()) {
            if (e.getValue().getTimeLimit() < t) {
                rslt++;
                objects.remove(e.getKey());
            }
        }
        return rslt;
    }

}
