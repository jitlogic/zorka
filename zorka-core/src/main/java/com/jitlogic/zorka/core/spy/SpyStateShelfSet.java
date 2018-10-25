package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.LimitedTime;

import java.util.HashMap;
import java.util.Map;

public class SpyStateShelfSet<T extends LimitedTime> {

    private Map<String,SpyStateShelf<T>> set = new HashMap<String, SpyStateShelf<T>>();

    public synchronized SpyStateShelf<T> get(String name) {
        if (!set.containsKey(name)) {
            set.put(name, new SpyStateShelf<T>());
        }
        return set.get(name);
    }

}
