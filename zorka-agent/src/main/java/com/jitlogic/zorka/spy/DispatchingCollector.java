/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.spy.collectors.SpyCollector;
import com.jitlogic.zorka.spy.processors.SpyArgProcessor;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.List;

import static com.jitlogic.zorka.spy.SpyConst.*;

public class DispatchingCollector implements SpyCollector {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    public synchronized  void collect(SpyRecord record) {

        if (SpyInstance.isDebugEnabled(SPD_CDISPATCHES)) {
            log.debug("Dispatching collector record: " + record);
        }

        SpyContext ctx = record.getContext();

        SpyDefinition sdef = ctx.getSpyDefinition();

        if (null == (record = process(sdef, record))) {
            return;
        }

        for (SpyCollector collector : sdef.getCollectors()) {
            try {
                collector.collect(record);
            } catch (Throwable e) {
                log.error("Error collecting record " + record, e);
            }
        }
    }

    private SpyRecord process(SpyDefinition sdef, SpyRecord record) {
        List<SpyArgProcessor> argProcessors = sdef.getTransformers(ON_COLLECT);

        for (SpyArgProcessor argProcessor : argProcessors) {
            try {
                if (null == (record = argProcessor.process(record))) {
                    break;
                }
            } catch (Throwable e) {
                log.error("Error transforming record: " + record + " (on processor " + argProcessor + ")", e);
                return null;
            }
        }

        return record;
    }


}
