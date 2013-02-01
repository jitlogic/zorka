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

import com.jitlogic.zorka.common.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteBufferUnitTest {

    @Test
    public void testEncodeDecodeNonEmptyString() {
        ByteBuffer buf = new ByteBuffer();
        buf.putString("oja!");

        assertEquals("oja!", new ByteBuffer(buf.getContent()).getString());
    }

    @Test
    public void testEncodeDecodeNullString() {
        ByteBuffer buf = new ByteBuffer();
        buf.putString(null);

        assertNull(new ByteBuffer(buf.getContent()).getString());
    }

    @Test
    public void testEncodeDecodeEmptyString() {
        ByteBuffer buf = new ByteBuffer();
        buf.putString("");

        assertEquals("", new ByteBuffer(buf.getContent()).getString());
    }

    @Test
    public void testEncodeDecodeOverflow1() {
        ByteBuffer buf1 = new ByteBuffer(10);
        buf1.putString("abcd");
        buf1.putString("defg");

        ByteBuffer buf2 = new ByteBuffer(buf1.getContent());

        assertEquals("abcd", buf2.getString());
        assertEquals("defg", buf2.getString());
    }

    @Test
    public void testEncodeDecodeOverflow2() {
        ByteBuffer buf1 = new ByteBuffer(8);
        buf1.putString("abcd");
        buf1.putString("1234567890");

        ByteBuffer buf2 = new ByteBuffer(buf1.getContent());

        assertEquals("abcd", buf2.getString());
        assertEquals("1234567890", buf2.getString());
    }
}
