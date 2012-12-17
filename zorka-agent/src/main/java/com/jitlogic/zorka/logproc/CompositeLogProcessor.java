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
package com.jitlogic.zorka.logproc;

import com.jitlogic.zorka.util.ZorkaLogLevel;

import java.util.Arrays;
import java.util.List;

public class CompositeLogProcessor implements LogProcessor {

    private boolean filter;
    private List<LogProcessor> processors;
    private ZorkaLogLevel logLevel = ZorkaLogLevel.TRACE;


    public CompositeLogProcessor(boolean filter, LogProcessor...processors) {
        this.filter = filter;
        this.processors = Arrays.asList(processors);
    }


    public LogRecord process(LogRecord rec) {

        if (rec == null || rec.getLogLevel().getPriority() < logLevel.getPriority()) {
            return filter ? null : rec;
        }

        for (LogProcessor processor : processors) {
            if (filter) {
                rec = processor.process(rec);
            } else {
                processor.process(rec);
            }

            if (rec == null) {
                break;
            }
        }

        return rec;
    }
}
