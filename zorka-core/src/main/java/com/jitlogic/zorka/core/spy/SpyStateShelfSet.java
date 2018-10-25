package com.jitlogic.zorka.core.spy;

import java.util.HashMap;
import java.util.Map;

public class SpyStateShelfSet {

    private Map<String,SpyStateShelf> set = new HashMap<String, SpyStateShelf>();

    public synchronized SpyStateShelf get(String name) {
        if (!set.containsKey(name)) {
            set.put(name, new SpyStateShelf());
        }
        return set.get(name);
    }

}
