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
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;
import java.util.Set;


public class SetFilterProcessor implements SpyProcessor {

    private static final ZorkaLog log = ZorkaLogger.getLog(SetFilterProcessor.class);

    private String srcField;
    private boolean invert;
    private Set<?> candidates;

    public SetFilterProcessor(String srcField, boolean invert, Set<?> candidates) {
        this.srcField = srcField;
        this.invert = invert;
        this.candidates = candidates;

        log.info(ZorkaLogger.ZSP_CONFIG, "SetFilterProcessor configured for "
                + srcField + ", candidates=" + this.candidates);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = record.get(srcField);
        boolean pass = candidates.contains(val) ^ invert;
        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZSP_ARGPROC, "pass(" + val + ":" + val.getClass().getName() + ") -> " + pass);
        }
        return pass ? record : null;
    }
}
