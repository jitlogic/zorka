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

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.List;

import static com.jitlogic.zorka.spy.SpyConst.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

public class DispatchingCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SpyDefinition sdef = null;


    public DispatchingCollector() {
    }

    public DispatchingCollector(SpyDefinition sdef) {
        this.sdef = sdef;
    }

    public synchronized SpyRecord process(int stage, SpyRecord record) {

        if (SpyInstance.isDebugEnabled(SPD_CDISPATCHES)) {
            log.debug("Dispatching collector record: " + record);
        }

        SpyContext ctx = record.getContext();

        SpyDefinition sd = sdef != null ? sdef : ctx.getSpyDefinition();

        if (null == (record = process(sd, record))) {
            return null;
        }

        for (SpyProcessor collector : sd.getCollectors()) {
            try {
                collector.process(SpyLib.ON_COLLECT, record);
            } catch (Throwable e) {
                log.error("Error collecting record " + record, e);
            }
        }

        return record;
    }

    private SpyRecord process(SpyDefinition sdef, SpyRecord record) {
        List<SpyProcessor> processors = sdef.getProcessors(ON_COLLECT);

        for (SpyProcessor processor : processors) {
            try {
                if (null == (record = processor.process(ON_COLLECT, record))) {
                    break;
                }
            } catch (Throwable e) {
                log.error("Error transforming record: " + record + " (on processor " + processor + ")", e);
                return null;
            }
        }

        return record;
    }


}
