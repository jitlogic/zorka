/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.spy.SpyLib;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Logs incoming records using trapper.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TrapperCollector implements SpyProcessor {

    private static ZorkaLog log = ZorkaLogger.getLog(TrapperCollector.class);

    /**
     * Trapper object.
     */
    private ZorkaTrapper trapper;


    /**
     * Log level
     */
    private ZorkaLogLevel logLevel;


    /**
     * Tag, message, error templates and error field
     */
    private String tag, message, errExpr, errField;


    /**
     * Creates new trapper collector
     *
     * @param trapper  output trapper
     * @param logLevel log level
     * @param tag      log tag
     * @param message  log message
     * @param errExpr  error expression
     * @param errField error field
     */
    public TrapperCollector(ZorkaTrapper trapper, ZorkaLogLevel logLevel,
                            String tag, String message, String errExpr, String errField) {

        this.trapper = trapper;
        this.logLevel = logLevel;
        this.tag = tag;
        this.message = message;
        this.errExpr = errExpr;
        this.errField = errField;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        if (trapper == null || logLevel == null || tag == null || message == null) {
            log.error(ZorkaLogger.ZSP_CONFIG,
                    "Improperly configured TrapperCollector (null trapper or log level or tag or message).");
            return record;
        }

        String tag = ObjectInspector.substitute(this.tag, record);
        String msg;

        if (errExpr != null) {
            msg = ObjectInspector.substitute(
                    0 != ((Integer) record.get(".STAGES") & (1 << SpyLib.ON_ERROR)) ? errExpr : message, record);
        } else {
            msg = ObjectInspector.substitute(message, record);
        }

        trapper.trap(logLevel, tag, msg, (Throwable) record.get(errField));

        return record;
    }
}
