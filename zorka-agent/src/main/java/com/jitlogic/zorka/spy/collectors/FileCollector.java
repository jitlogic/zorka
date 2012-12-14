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
package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.agent.FileTrapper;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLogLevel;

public class FileCollector implements SpyProcessor {

    private FileTrapper trapper;
    private String expr;
    private ZorkaLogLevel logLevel;
    private String tag;

    private ObjectInspector inspector = new ObjectInspector();

    public FileCollector(FileTrapper trapper, String expr, ZorkaLogLevel logLevel, String tag) {
        this.trapper = trapper;
        this.expr = expr;
        this.logLevel = logLevel;
        this.tag = tag;
    }

    public SpyRecord process(int stage, SpyRecord record) {
        Object[] vals = record.getVals(SpyLib.ON_COLLECT);

        String msg = inspector.substitute(expr, vals);

        trapper.log(tag, logLevel, msg, null);

        return record;
    }

}
