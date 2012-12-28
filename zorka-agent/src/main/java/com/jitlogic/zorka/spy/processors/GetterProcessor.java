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
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import static com.jitlogic.zorka.spy.SpyLib.SPD_ARGPROC;

/**
 * Digs deeper into an object the same way zorka.jmx() does.
 */
public class GetterProcessor implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private String src, dst;
    private Object[] path;
    private ObjectInspector inspector = new ObjectInspector();


    public GetterProcessor(String src, String dst, Object... path) {
        this.src = src; this.dst = dst;
        this.path = path;
    }


    public SpyRecord process(SpyRecord record) {
        Object val = record.get(src);

        for (Object obj : path) {
            if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
                log.debug("Descending into '" + obj + "' of '" + val + "' ...");
            }
            val = inspector.get(val, obj);
        }

        if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
            log.debug("Final result: '" + val + "' stored to slot " + dst);
        }

        record.put(dst, val);

        return record;
    }
}
