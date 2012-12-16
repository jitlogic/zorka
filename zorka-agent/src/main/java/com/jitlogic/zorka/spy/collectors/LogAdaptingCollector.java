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

import com.jitlogic.zorka.logproc.LogAdapter;
import com.jitlogic.zorka.logproc.LogProcessor;
import com.jitlogic.zorka.logproc.LogRecord;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;

public class LogAdaptingCollector implements SpyProcessor {

    private int stage, slot;
    private LogAdapter adapter;
    private LogProcessor processor;


    public LogAdaptingCollector(int[] src, LogProcessor processor) {
        stage = src[0]; slot = src[1];
        this.adapter = new LogAdapter();
        this.processor = processor;
    }


    public SpyRecord process(int stage, SpyRecord record) {

        LogRecord rec = adapter.toLogRecord(record.get(SpyLib.fs(this.stage, stage), slot));

        processor.process(rec);

        return record;
    }

}
