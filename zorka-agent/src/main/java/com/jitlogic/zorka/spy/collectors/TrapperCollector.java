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

import com.jitlogic.zorka.logproc.ZorkaLogLevel;
import com.jitlogic.zorka.logproc.ZorkaTrapper;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

public class TrapperCollector implements SpyProcessor {

    private ZorkaTrapper trapper;
    private String tagExpr, stdExpr, errExpr, errSlot;

    public TrapperCollector(ZorkaTrapper trapper, String tagExpr, String stdExpr, String errExpr, String errField) {
        this.trapper = trapper;
        this.tagExpr = tagExpr;
        this.stdExpr = stdExpr;
        this.errExpr = errExpr;
        this.errSlot = errField;
    }


    public SpyRecord process(SpyRecord record) {

        String tag = ObjectInspector.substitute(tagExpr, record);
        String msg = ObjectInspector.substitute(record.hasStage(SpyLib.ON_ERROR) ? errExpr : stdExpr, record);

        trapper.trap(ZorkaLogLevel.DEBUG, tag, msg, (Throwable)record.get(errSlot));

        return record;
    }
}
