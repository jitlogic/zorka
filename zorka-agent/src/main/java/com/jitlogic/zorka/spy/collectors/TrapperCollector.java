/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.integ.ZorkaLogLevel;
import com.jitlogic.zorka.integ.ZorkaTrapper;
import com.jitlogic.zorka.api.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

/**
 * Logs incoming records using trapper.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TrapperCollector implements SpyProcessor {

    /** Trapper object. */
    private ZorkaTrapper trapper;

    /** Log level */
    private ZorkaLogLevel logLevel;

    /** Tag, message, error templates and error field */
    private String tagExpr, stdExpr, errExpr, errField;

    public TrapperCollector(ZorkaTrapper trapper, ZorkaLogLevel logLevel,
                            String tagExpr, String stdExpr, String errExpr, String errField) {

        this.trapper = trapper;
        this.logLevel = logLevel;
        this.tagExpr = tagExpr;
        this.stdExpr = stdExpr;
        this.errExpr = errExpr;
        this.errField = errField;
    }

    @Override
    public SpyRecord process(SpyRecord record) {

        String tag = ObjectInspector.substitute(tagExpr, record);
        String msg = ObjectInspector.substitute(record.hasStage(SpyLib.ON_ERROR) ? errExpr : stdExpr, record);

        trapper.trap(logLevel, tag, msg, (Throwable)record.get(errField));

        return record;
    }
}
