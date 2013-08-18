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
package com.jitlogic.zorka.central.test;

import com.jitlogic.zorka.central.rds.RAGZInputStream;
import com.jitlogic.zorka.central.rds.RAGZOutputStream;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import com.jitlogic.zorka.common.test.support.TestUtil;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.central.CentralUtil.fromUIntBE;
import static com.jitlogic.zorka.central.CentralUtil.toUIntBE;

public class RAGZUnitTest extends CentralFixture {

    public static final String GZIP = "/usr/bin/gzip";

    @Test
    public void testTrivialCompressAndCheckHeaders() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        os.close();

        assertThat(new File(path).length()).isEqualTo(30);

        // TODO check important internal fields here
        byte[] buf = TestUtil.cat(path);
        assertThat(buf[16]).isEqualTo((byte) 2);
        assertThat(buf[19]).isEqualTo((byte) 0);

        if (new File(GZIP).canExecute()) {
            assertThat(TestUtil.cmd(GZIP + " -d " + path)).isEqualTo(0);
            assertThat(new File(tmpFile("test")).length()).isEqualTo(0);
        }
    }


    @Test
    public void testCompressSingleSegmentAndCheckHeaders() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        os.write("ABCD".getBytes());
        os.close();

        byte[] buf = TestUtil.cat(path);


        assertThat(new File(path).length()).isEqualTo(40);
        //assertThat(buf[16]).isEqualTo((byte)6);
        //assertThat(buf[19]).isEqualTo((byte)0);
        //assertThat(buf[30]).isEqualTo((byte)4);
        //assertThat(buf[33]).isEqualTo((byte)0);

        if (new File(GZIP).canExecute()) {
            assertThat(TestUtil.cmd(GZIP + " -d " + path)).isEqualTo(0);
            assertThat(new File(tmpFile("test")).length()).isEqualTo(4);
        }
    }


    @Test
    public void testWriteReadSingleSegment() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        os.write("ABCD".getBytes());
        os.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        assertThat(is.logicalLength()).isEqualTo(4);

        byte[] buf = new byte[4];
        assertThat(is.read(buf)).isEqualTo(4);

        assertThat(new String(buf, "UTF-8")).isEqualTo("ABCD");
        is.close();
    }


    @Test
    public void testReadWriteMultiSegment() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path, 8);
        os.write("1234567890".getBytes());
        os.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        assertThat(is.logicalLength()).isEqualTo(10);

        byte[] buf = new byte[10];
        assertThat(is.read(buf)).isEqualTo(10);

        assertThat(new String(buf, "UTF-8")).isEqualTo("1234567890");
        is.close();
    }

    private String randStr() {
        StringBuffer sb = new StringBuffer();
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            sb.append(r.nextInt() % 10);
        }
        return sb.toString();
    }


    @Test
    public void testReadWritePartialSegment() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        for (int i = 0; i < 500; i++) {
            os.write(randStr().getBytes());
        }

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        assertThat(is.logicalLength()).isGreaterThan(0);

        is.close();
        os.close();
    }

    @Test
    public void testReopenAndAddSomeContent() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        os.write("1234".getBytes());
        os.close();

        os = RAGZOutputStream.toFile(path);
        os.write("ABCD".getBytes());
        os.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        assertThat(is.logicalLength()).isEqualTo(8);
        byte[] buf = new byte[8];
        assertThat(is.read(buf)).isEqualTo(8);
        assertThat(new String(buf, "UTF-8")).isEqualTo("1234ABCD");
        is.close();

    }

    @Test
    public void testUIntEncoding() {
        byte[] b = fromUIntBE(4000000L);
        long l = toUIntBE(b);
        assertEquals(4L, toUIntBE(fromUIntBE(4L)));
        assertEquals(4000L, toUIntBE(fromUIntBE(4000L)));
        assertEquals(4000000L, toUIntBE(fromUIntBE(4000000L)));
        assertEquals(4000000000L, toUIntBE(fromUIntBE(4000000000L)));
    }

    //@Test
    public void testReopenImproperlyClosedSegment() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");
        RAGZOutputStream os = new RAGZOutputStream(f, RAGZOutputStream.DEFAULT_SEGSZ);
        os.write("ABCD".getBytes());
        f.close();

        os = RAGZOutputStream.toFile(path);
        os.write("1234".getBytes());
        os.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        assertThat(is.logicalLength()).isEqualTo(4);
        byte[] buf = new byte[4];
        assertThat(is.read(buf)).isEqualTo(4);
        assertThat(new String(buf, "UTF-8")).isEqualTo("1234");
    }

}
