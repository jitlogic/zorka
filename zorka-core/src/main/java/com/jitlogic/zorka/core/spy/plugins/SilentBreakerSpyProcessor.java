package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SilentBreakerSpyProcessor implements SpyProcessor {

    private Logger log = LoggerFactory.getLogger(SilentBreakerSpyProcessor.class);

    private SpyProcessor[] processors;

    public SilentBreakerSpyProcessor(SpyProcessor[] processors) {
        this.processors = processors;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        try {
            for (SpyProcessor p : processors) {
                if (p != null) {
                    p.process(record);
                }
            }
            return record;
        } catch (Throwable e) {
            if (log.isTraceEnabled()) {
                log.trace("Dropping trace " + record + " due to exception thrown: " + e.getMessage(), e);
            } else if (log.isDebugEnabled()) {
                log.debug("Dropping trace " + record + " due to exception thrown: " + e.getMessage());
            }
            return null;
        }
    }
}
