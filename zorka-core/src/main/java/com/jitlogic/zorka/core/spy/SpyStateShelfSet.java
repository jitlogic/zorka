package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.LimitedTime;

import java.util.HashMap;
import java.util.Map;

public class SpyStateShelfSet<K,T extends LimitedTime> {

    private Map<String,SpyStateShelf<K,T>> set = new HashMap<String, SpyStateShelf<K,T>>();

    public synchronized SpyStateShelf<K,T> get(String name) {
        if (!set.containsKey(name)) {
            set.put(name, new SpyStateShelf<K,T>());
        }
        return set.get(name);
    }

}
