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

import com.jitlogic.zorka.logproc.LogProcessor;
import com.jitlogic.zorka.logproc.LogRecord;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLogLevel;

import static com.jitlogic.zorka.spy.SpyLib.ON_COLLECT;

public class LogFormattingCollector implements SpyProcessor {

    private ObjectInspector inspector = new ObjectInspector();

    private String levelTmpl, msgTmpl, classTmpl, methodTmpl, excTmpl;

    private LogProcessor processor;

    public LogFormattingCollector(String levelTmpl, String msgTmpl) {
        this(levelTmpl, msgTmpl, "${shortClassName}", "${methodName}", "");
    }

    public LogFormattingCollector(String levelTmpl, String msgTmpl, String classTmpl, String methodTmpl, String excTmpl) {
        this.levelTmpl = levelTmpl;
        this.msgTmpl = msgTmpl;
        this.classTmpl = classTmpl;
        this.methodTmpl = methodTmpl;
        this.excTmpl = excTmpl;
    }

    private ZorkaLogLevel parseLevel(SpyRecord record) {
        try {
            return ZorkaLogLevel.valueOf(
                    inspector.substitute(levelTmpl, record, ON_COLLECT).toUpperCase().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public SpyRecord process(int stage, SpyRecord record) {

            ZorkaLogLevel level = parseLevel(record);

            if (level != null) {
                LogRecord rec = new LogRecord(level,
                    inspector.substitute(classTmpl, record, ON_COLLECT),
                    inspector.substitute(methodTmpl, record, ON_COLLECT),
                    inspector.substitute(msgTmpl, record, ON_COLLECT),
                    inspector.substitute(excTmpl, record, ON_COLLECT));
                processor.process(rec);
            }
        return record;
    }
}
