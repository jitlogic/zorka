/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.normproc.Normalizer;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Normalizes value of a field from a record and saves it in another field (eg. SQL or LDAP query).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class NormalizingProcessor implements SpyProcessor {

    /**
     * Logger
     */
    private static final ZorkaLog log = ZorkaLogger.getLog(NormalizingProcessor.class);

    /**
     * Source field
     */
    private String src;

    /**
     * Destination field
     */
    private String dst;

    /**
     * Normalizer
     */
    private Normalizer normalizer;


    /**
     * Creates normalizing record processor.
     *
     * @param src        source field
     * @param dst        destination field
     * @param normalizer normalizer object
     */
    public NormalizingProcessor(String src, String dst, Normalizer normalizer) {
        this.src = src;
        this.dst = dst;
        this.normalizer = normalizer;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        Object v = record.get(src);

        String s = (v instanceof String) ? normalizer.normalize((String) v) : null;

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZAG_TRACES, "Normalizing: '" + v + "' -> '" + s + "'");
        }

        record.put(dst, s);

        return record;
    }
}
