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

package com.jitlogic.zorka.viewer.test;

import com.jitlogic.zorka.viewer.TraceDataSet;

import com.jitlogic.zorka.viewer.ViewerTraceRecord;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

public class TracePrinterManualTest {

    @Test
    public void testReadAndPrintTrace() throws Exception {
        String path = this.getClass().getResource("/trace.ztr").getPath();
        TraceDataSet tds = new TraceDataSet(new File(path));
        List<ViewerTraceRecord> lst = tds.getRecords();
        assertTrue(lst.size() > 0);
    }

}
