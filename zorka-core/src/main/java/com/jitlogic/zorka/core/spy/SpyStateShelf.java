package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.LimitedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpyStateShelf<T extends LimitedTime> {

    private static Logger log = LoggerFactory.getLogger(SpyStateShelf.class);

    private Map<Integer,T> objects = new ConcurrentHashMap<Integer, T>();

    public synchronized void shelve(Object key, T obj) {
        int hash = key.hashCode();
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Shelving: " + key + " (hash=" + hash + "), obj=" + obj);
        }
        objects.put(hash, obj);
    }

    public synchronized T unshelve(Object key) {
        int hash = key.hashCode();
        T obj = objects.get(hash);
        if (log.isDebugEnabled()) {
            log.debug("[" + this + "] Unshelving: " + key + " (hash=" + hash + "), obj=" + obj);
        }
        if (obj != null) objects.remove(hash);
        return obj;
    }

    public synchronized int cleanup() {
        int rslt = 0;
        long t = System.currentTimeMillis();
        for (Map.Entry<Integer,? extends LimitedTime> e : objects.entrySet()) {
            if (e.getValue().getTimeLimit() < t) {
                rslt++;
                objects.remove(e.getKey());
            }
        }
        return rslt;
    }

}
