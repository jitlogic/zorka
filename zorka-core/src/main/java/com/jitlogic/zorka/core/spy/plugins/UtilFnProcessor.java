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
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

public class UtilFnProcessor implements SpyProcessor {

    private final static ZorkaLog log = ZorkaLogger.getLog(UtilFnProcessor.class);

    public static final int UF_STRTIME = 1;
    public static final int UF_STRCLOCK = 2;

    private int function;
    private String srcField, dstField;

    public static UtilFnProcessor strClockFn(String dst, String src) {
        return new UtilFnProcessor(UF_STRCLOCK, src, dst);
    }

    public static UtilFnProcessor strTimeFn(String dst, String src) {
        return new UtilFnProcessor(UF_STRTIME, src, dst);
    }

    private UtilFnProcessor(int function, String src, String dst) {
        this.function = function;
        this.srcField = src;
        this.dstField = dst;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        switch (function) {
            case UF_STRTIME: {
                Object t = record.get(srcField);
                if (t instanceof Long) {
                    record.put(dstField, ZorkaUtil.strTime((Long) t));
                } else {
                    log.error(ZorkaLogger.ZSP_ERRORS, "Cannot process value " + t + ": must be of type Long.");
                }
                break;
            }
            case UF_STRCLOCK: {
                Object t = record.get(srcField);
                if (t instanceof Long) {
                    record.put(dstField, ZorkaUtil.strClock((Long) t));
                } else {
                    log.error(ZorkaLogger.ZSP_ERRORS, "Cannot process value " + t + ": must be of type Long.");
                }
                break;
            }
            default:
                log.error(ZorkaLogger.ZSP_ERRORS, "Invalid function id: " + function);
        }
        return record;
    }
}
