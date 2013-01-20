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

import com.jitlogic.zorka.common.SimpleTraceFormat;
import com.jitlogic.zorka.viewer.TracePrinter;
import org.junit.Test;

import java.io.*;

public class TracePrinterManualTest {

    @Test
    public void testReadAndPrintTrace() throws Exception {
        File f = new File("/tmp/trace.trc");
        InputStream is = new FileInputStream(f);
        byte[] buf = new byte[(int)f.length()];
        is.read(buf);
        is.close();

        OutputStream os = new BufferedOutputStream(new FileOutputStream("/tmp/trace.txt"));
        PrintStream ps = new PrintStream(os);

        TracePrinter printer = new TracePrinter(ps);
        new SimpleTraceFormat(buf).decode(printer);
        ps.flush();
        ps.close();

    }

}
