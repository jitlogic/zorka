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

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLogLevel;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

public class ZorkaLogCollector implements SpyProcessor {

    private ZorkaLogger logger;
    private ZorkaLogLevel logLevel;
    private String tag, message;
    private String fCond, fErr;


    public ZorkaLogCollector(ZorkaLogLevel logLevel, String tag, String message, String fCond, String fErr) {
        this.logger = ZorkaLogger.getLogger();
        this.logLevel = logLevel;
        this.tag = tag;
        this.message = message;
        this.fCond = fCond;
        this.fErr = fErr;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        if (fCond != null) {
            Object v = record.get(fCond);
            if (v == null || Boolean.FALSE.equals(v)) {
                return record;
            }
        }

        Throwable e = null;

        if (fErr != null && record.get(fErr) instanceof Throwable) {
            e = (Throwable) record.get(fErr);
        }

        logger.trap(logLevel, tag, ObjectInspector.substitute(message, record), e);

        return record;
    }

}
