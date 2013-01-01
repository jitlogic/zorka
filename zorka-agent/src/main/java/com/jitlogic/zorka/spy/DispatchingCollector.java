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

import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.api.SpyLib.*;

public class DispatchingCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SpyDefinition sdef = null;


    public DispatchingCollector() {
    }

    public DispatchingCollector(SpyDefinition sdef) {
        this.sdef = sdef;
    }

    public synchronized Map<String,Object> process(Map<String,Object> record) {

        if (SpyInstance.isDebugEnabled(SPD_CDISPATCHES)) {
            log.debug("Dispatching collector record: " + record);
        }

        SpyContext ctx = (SpyContext) record.get(".CTX");

        SpyDefinition sd = sdef != null ? sdef : ctx.getSpyDefinition();

        if (null == (record = process(sd, record))) {
            return null;
        }

        return record;
    }

    private Map<String,Object> process(SpyDefinition sdef, Map<String,Object> record) {
        List<SpyProcessor> processors = sdef.getProcessors((Integer) record.get(".STAGE"));

        for (SpyProcessor processor : processors) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error transforming record: " + record + " (on processor " + processor + ")", e);
                return null;
            }
        }

        return record;
    }


}
