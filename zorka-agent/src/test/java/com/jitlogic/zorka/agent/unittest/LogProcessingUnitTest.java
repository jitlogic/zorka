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
package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;

import com.jitlogic.zorka.logproc.LogAdapter;
import com.jitlogic.zorka.logproc.LogFilter;
import com.jitlogic.zorka.logproc.LogRecord;
import com.jitlogic.zorka.util.ZorkaLogLevel;
import org.junit.Test;

import java.util.logging.Level;

import static org.junit.Assert.*;

public class LogProcessingUnitTest extends ZorkaFixture {

    @Test
    public void testConvertJdkToZorkaLogRecordAttrs() {
        LogAdapter adapter = new LogAdapter();
        java.util.logging.LogRecord obj = new java.util.logging.LogRecord(Level.FINEST, "oja!");



        LogRecord record = adapter.toLogRecord(obj);
        assertEquals(ZorkaLogLevel.TRACE, record.getLogLevel());
        assertEquals("oja!", record.getMessage());
    }


    @Test
    public void testConvertJdkToZorkaLogRecordCheckLevels() {
        LogAdapter adapter = new LogAdapter();
        java.util.logging.LogRecord rec = new java.util.logging.LogRecord(Level.FINEST, "oja!");

        assertEquals(ZorkaLogLevel.TRACE, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.FINER);
        assertEquals(ZorkaLogLevel.DEBUG, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.FINE);
        assertEquals(ZorkaLogLevel.DEBUG, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.CONFIG);
        assertEquals(ZorkaLogLevel.INFO, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.INFO);
        assertEquals(ZorkaLogLevel.INFO, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.WARNING);
        assertEquals(ZorkaLogLevel.WARN, adapter.toLogRecord(rec).getLogLevel());

        rec.setLevel(Level.SEVERE);
        assertEquals(ZorkaLogLevel.ERROR, adapter.toLogRecord(rec).getLogLevel());
    }

    @Test
    public void testLogFilterByClass() {
        LogRecord rec = new LogRecord(ZorkaLogLevel.ERROR, "com.jitlogic.SomeClass", "someMethod", "oja!");

        assertNotNull(new LogFilter(LogFilter.FILTER_CLASS, "com.jitlogic.SomeClass").process(rec));
        assertNull(new LogFilter(LogFilter.FILTER_CLASS, "com.jitlogic.SomeClassExt").process(rec));
        assertNotNull(new LogFilter(LogFilter.FILTER_CLASS, "com.jitlogic.*").process(rec));
        assertNull(new LogFilter(LogFilter.FILTER_CLASS, "com.*").process(rec));
        assertNotNull(new LogFilter(LogFilter.FILTER_CLASS, "com.**").process(rec));
    }
}
