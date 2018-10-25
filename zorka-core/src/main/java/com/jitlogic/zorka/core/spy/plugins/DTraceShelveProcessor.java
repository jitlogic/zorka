package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.SpyStateShelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.jitlogic.zorka.core.spy.TracerLib.DTRACE_IN;
import static com.jitlogic.zorka.core.spy.TracerLib.DTRACE_STATE;
import static com.jitlogic.zorka.core.spy.TracerLib.DTRACE_UUID;

public class DTraceShelveProcessor implements SpyProcessor {

    private static Logger log = LoggerFactory.getLogger(DTraceShelveProcessor.class);

    private SpyStateShelf<Integer,DTraceState> shelf;
    private String keyAttr;
    private long timeout;
    private boolean shelve;

    public DTraceShelveProcessor(SpyStateShelf<Integer,DTraceState> shelf, String keyAttr, long timeout, boolean shelve) {
        this.shelf = shelf;
        this.keyAttr = keyAttr;
        this.timeout = timeout;
        this.shelve = shelve;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        Integer key = rec.get(keyAttr).hashCode();

        if (key != null) {
            if (shelve) {
                Object obj = rec.get(DTRACE_STATE);
                if (obj instanceof DTraceState) {
                    DTraceState state = (DTraceState) obj;
                    if (log.isDebugEnabled()) {
                        log.debug("Shelving DTRACE state: key=" + key + ", hash=" + key.hashCode() + ", state=" + state +
                                ", timeout=" + timeout + ", shelf=" + shelf);
                    }
                    state.setTimeout(timeout);
                    shelf.shelve(key, state);
                }
            } else {
                DTraceState state = shelf.unshelve(key);
                if (log.isDebugEnabled()) {
                    log.debug("Unshelving DTRACE state: key=" + key + ", hash=" + key.hashCode() + ", state=" + state +
                        ", shelf=" +shelf);
                }
                rec.put(DTRACE_UUID, state.getUuid());
                rec.put(DTRACE_IN, state.getTid());
            }
        }

        return rec;
    }
}
