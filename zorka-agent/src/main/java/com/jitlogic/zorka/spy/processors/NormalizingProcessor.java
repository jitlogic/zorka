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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.normproc.Normalizer;

import java.util.Map;

import static com.jitlogic.zorka.api.SpyLib.SPD_ARGPROC;

public class NormalizingProcessor implements SpyProcessor {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private String src, dst;
    private Normalizer normalizer;


    public NormalizingProcessor(String src, String dst, Normalizer normalizer) {
        this.src = src; this.dst = dst;
        this.normalizer = normalizer;
    }


    public Map<String,Object> process(Map<String,Object> record) {

        Object v = record.get(src);

        String s = (v instanceof String) ? normalizer.normalize((String)v) : null;

        if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
            log.debug("Normalizing: '" + v + "' -> '" + s + "'");
        }

        record.put(dst, s);

        return record;
    }
}
