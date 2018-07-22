/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.test;


import com.jitlogic.zorka.common.util.TapInputStream;

import com.jitlogic.zorka.common.util.TapOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class StreamTappingUnitTest {


    @Test
    public void testTapInputStreamReadChars() throws Exception {
        TapInputStream tis = new TapInputStream(new ByteArrayInputStream("ABCD".getBytes()));

        assertEquals(65, tis.read());
        assertEquals(66, tis.read());

        assertEquals("AB", tis.asString());
    }


    @Test
    public void testTapInputStreamReadArray() throws Exception {
        TapInputStream tis = new TapInputStream(new ByteArrayInputStream("ABCD".getBytes()));

        assertEquals(4, tis.read(new byte[4]));

        assertEquals("ABCD", tis.asString("UTF-8"));
    }


    @Test
    public void testTapInputStreamReadArrayWithOffset() throws Exception {
        TapInputStream tis = new TapInputStream(new ByteArrayInputStream("ABCD".getBytes()));

        assertEquals(2, tis.read(new byte[4], 2, 2));

        assertEquals("AB", tis.asString());
    }


    @Test
    public void testTapInputStreamWithMarkAndReset() throws Exception {
        TapInputStream tis = new TapInputStream(new ByteArrayInputStream("ABCDEF".getBytes()));

        tis.read(new byte[2]); tis.mark(100);
        tis.read(new byte[2]); tis.reset();
        tis.read(new byte[3]);

        assertEquals("ABCDE", tis.asString());
    }


    @Test
    public void testTapOutpuStreamWriteChars() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TapOutputStream tos = new TapOutputStream(os);

        tos.write(65); tos.write(66);

        assertEquals("AB", tos.asString());
        assertEquals("AB", new String(os.toByteArray()));
    }


    @Test
    public void testTapOutputStreamWriteArray() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TapOutputStream tos = new TapOutputStream(os);

        tos.write("ABC".getBytes());

        assertEquals("ABC", tos.asString());
        assertEquals("ABC", new String(os.toByteArray()));
    }


    @Test
    public void testTapOutputStreamWriteArrayWithOffset() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TapOutputStream tos = new TapOutputStream(os);

        tos.write("ABCD".getBytes(), 1, 2);

        assertEquals("BC", tos.asString());
        assertEquals("BC", new String(os.toByteArray()));
    }
}
