package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TracerLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

import java.util.List;
import java.util.Map;

public class ProcAttrProcessor implements SpyProcessor {

    private static Logger log = LoggerFactory.getLogger(ProcAttrProcessor.class);

    private TracerLib tracerLib;
    private int flags;
    private String prefix;
    private String src;
    private String[] path;

    public ProcAttrProcessor(TracerLib tracerLib, int flags, String prefix, String src, String...path) {
        this.tracerLib = tracerLib;
        this.flags = flags;
        this.prefix = prefix;
        this.src = src;
        this.path = path;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {

        Object o = ObjectInspector.get(rec.get(src));

        for (String p : path) {
            o = ObjectInspector.getAttr(o, p);
        }

        if (o == null) {
            if (0 != (flags & PA_WARN_ON_NULL))
                log.warn("Got NULL when processing args: " + rec);
            return rec;
        }

        if (0 != (flags & PA_MAP)) {
            if (!(o instanceof Map)) {
                return rec;
            }
            return processMap(rec, (Map) o);
        } else if (0 != (flags & PA_LIST)) {
            if (!(o instanceof List)) {
                return rec;
            }
            return processList(rec, (List) o);
        }

        return rec;
    }

    private Map<String, Object> processMap(Map<String, Object> rec, Map<?,?> m) {
        switch (flags & PA_MAP) {
            case PA_MAP_OF_ANY:
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    tracerLib.newAttr(prefix + e.getKey(), cast(e.getValue()));
                }
                break;
            case PA_MAP_OF_LISTS_1:
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getValue() instanceof List) {
                        List l = (List) e.getValue();
                        if (!l.isEmpty()) {
                            tracerLib.newAttr(prefix + e.getKey(), cast(l.get(0)));
                        }
                    }
                }
                break;
            case PA_MAP_OF_LISTS_N:
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getValue() instanceof List) {
                        List l = (List) e.getValue();
                        for (int i = 0; i < l.size(); i++) {
                            tracerLib.newAttr(prefix + e.getKey() + "." + i, cast(l.get(i)));
                        }
                    }
                }
                break;
            default:
                log.warn(String.format("Illegal map type flag: 0x%04x", (flags & PA_MAP)));
        }
        return rec;
    }

    private Map<String,Object> processList(Map<String,Object> rec, List<?> l) {
        switch (flags & PA_LIST) {
            case PA_LIST_OF_ANY:
                for (int i = 0; i < l.size(); i++) {
                    tracerLib.newAttr(prefix + "." + i, cast(l.get(i)));
                }
                break;
            default:
                log.warn(String.format("Illegal list type flag: 0x%04x", (flags & PA_LIST)));
        }
        return rec;
    }

    private Object cast(Object o) {
        return 0 != (flags & PA_TO_STRING) ? ""+o : o;
    }
}
