/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.test;

import com.jitlogic.zico.core.rds.RAGZInputStream;
import com.jitlogic.zico.core.rds.RAGZOutputStream;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.test.support.TestUtil;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Random;

import static com.jitlogic.zico.core.ZicoUtil.fromUIntBE;
import static com.jitlogic.zico.core.ZicoUtil.toUIntBE;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class RAGZUnitTest extends ZicoFixture {

    public static final String GZIP = "/usr/bin/gzip";

    private void checkGzipFile(String path) throws Exception {
        if (new File(GZIP).canExecute()) {
            assertThat(TestUtil.cmd(GZIP + " -d " + path)).isEqualTo(0);
            //assertThat(new File(tmpFile("test")).length()).isEqualTo(0);
        }
    }


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

        checkGzipFile(path);
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

        checkGzipFile(path);
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


    @Test
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
        assertThat(is.logicalLength()).isEqualTo(8);
        byte[] buf = new byte[4];
        assertThat(is.read(buf)).isEqualTo(4);
        assertThat(new String(buf, "UTF-8")).isEqualTo("ABCD");
        assertThat(is.read(buf)).isEqualTo(4);
        assertThat(new String(buf, "UTF-8")).isEqualTo("1234");
    }


    @Test
    public void testIfOutputStreamDropsEmptySegments() throws Exception {
        String path = tmpFile("test.gz");

        RAGZOutputStream os1 = RAGZOutputStream.toFile(path);
        os1.close();

        long len1 = new File(path).length();

        RAGZOutputStream os2 = RAGZOutputStream.toFile(path);
        os2.close();

        long len2 = new File(path).length();

        assertEquals("Second opening should not create another header.", len1, len2);
    }


    @Test
    public void testIfOutputStreamDropsEmptySegmentsAfterBeingImproperlyClosed() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");

        new RAGZOutputStream(f, RAGZOutputStream.DEFAULT_SEGSZ);
        f.close();

        RAGZOutputStream os2 = RAGZOutputStream.toFile(path);
        os2.close();

        assertEquals("Empty file should have exactly 30 bytes.", 30, new File(path).length());
    }


    @Test  // propably the most common case in production
    public void testIfOutputStreamIsProperlyReopenedAfterImproperlyClosedPartiallyWrittenSegment() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");

        RAGZOutputStream os1 = new RAGZOutputStream(f, RAGZOutputStream.DEFAULT_SEGSZ);
        os1.write("ABCDEFGH".getBytes());
        f.close();

        long len1 = new File(path).length();

        RAGZOutputStream os2 = RAGZOutputStream.toFile(path);
        os2.close();

        long len2 = new File(path).length();

        assertEquals("Footer (8B) + empty segment (30B) should be added.", len1 + 40, len2);


        RAGZOutputStream os3 = RAGZOutputStream.toFile(path);
        os3.close();

        long len3 = new File(path).length();

        assertEquals(len3, len2);

        checkGzipFile(path);
    }

    @Test  // propably the most common case in production
    public void testIfOutputStreamIsProperlyReopenedAfterImproperlyClosedPartiallyWrittenSegment2() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");

        RAGZOutputStream os1 = new RAGZOutputStream(f, RAGZOutputStream.DEFAULT_SEGSZ);
        os1.write("ABCDEFGH".getBytes());
        f.close();

        long len1 = new File(path).length();

        RAGZOutputStream os2 = RAGZOutputStream.toFile(path);
        os2.getOutFile().close();

        long len2 = new File(path).length();

        assertEquals("Footer (8B) + empty segment (30B) should be added.", len1 + 30, len2);


        RAGZOutputStream os3 = RAGZOutputStream.toFile(path);
        os3.close();

        long len3 = new File(path).length();

        assertEquals(len2 + 10, len3);

        checkGzipFile(path);
    }


    @Test
    public void testConcurrentReadAndWriteInTheSameFileLastSegment() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");

        RAGZOutputStream os1 = new RAGZOutputStream(f, RAGZOutputStream.DEFAULT_SEGSZ);

        RAGZInputStream is1 = RAGZInputStream.fromFile(path);
        byte[] b = new byte[4];

        os1.write("ABCD".getBytes());
        assertEquals(4, is1.read(b));
        assertEquals("ABCD", new String(b, "UTF8"));

        os1.write("EFGH".getBytes());
        assertEquals(4, is1.read(b));
        assertEquals("EFGH", new String(b, "UTF8"));
    }


    @Test
    public void testConcurrentReadAndWriteInTheSameFileNextSegment() throws Exception {
        String path = tmpFile("test.gz");
        RandomAccessFile f = new RandomAccessFile(path, "rw");

        RAGZOutputStream os1 = new RAGZOutputStream(f, 8);
        RAGZInputStream is1 = RAGZInputStream.fromFile(path);
        byte[] b = new byte[8];

        os1.write("ABCDEFGH".getBytes());
        assertEquals(8, is1.read(b));
        assertEquals("ABCDEFGH", new String(b, "UTF8"));

        os1.write("12345678".getBytes());
        assertEquals(8, is1.read(b));
        assertEquals("12345678", new String(b, "UTF8"));
    }


    @Test
    public void testRAGZOutputStreamLogicalLength() throws Exception {
        String path = tmpFile("test.gz");

        RAGZOutputStream os1 = RAGZOutputStream.toFile(path);
        os1.write("ABCD".getBytes());
        os1.close();

        RAGZOutputStream os2 = RAGZOutputStream.toFile(path);
        long llen = os2.logicalLength();
        os2.close();

        assertEquals(4, llen);
    }


    @Test
    public void testNonResetClenAtSegmentExtensionBug() throws Exception {
        String path = tmpFile("test.gz");

        RAGZOutputStream os = RAGZOutputStream.toFile(path);
        os.write("1234".getBytes());
        os.close();

        os = RAGZOutputStream.toFile(path);
        os.getOutFile().close();

        RandomAccessFile f = new RandomAccessFile(path, "rw");
        os = new RAGZOutputStream(f, 1024);
        os.write("5678".getBytes());
        //f.close();
        os.close();

        os = RAGZOutputStream.toFile(path);
        os.write("ABCD".getBytes());
        os.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        byte[] b = new byte[12];
        assertEquals(12, is.read(b));
        assertEquals("12345678ABCD", new String(b, "UTF8"));
        is.close();
    }


    @Test
    public void testRereadAfterLastSegmentHasBeenFinishedBUG() throws Exception {
        String path = tmpFile("test.gz");

        RandomAccessFile f = new RandomAccessFile(path, "rw");
        RAGZOutputStream os = new RAGZOutputStream(f, 4);
        os.write("123".getBytes());

        RAGZInputStream is = RAGZInputStream.fromFile(path);

        os.write("456".getBytes());
        os.write("ABC".getBytes());

        check(is, 4, "56");
        check(is, 7, "BC");

        is.close();
        os.close();
    }


    @Test
    public void testReadFromPreviousSegmentsBUG() throws Exception {
        String path = tmpFile("test.gz");

        RandomAccessFile f = new RandomAccessFile(path, "rw");
        RAGZOutputStream os = new RAGZOutputStream(f, 4);
        os.write("123".getBytes());
        os.write("456".getBytes());
        os.write("ABC".getBytes());

        RAGZInputStream is = RAGZInputStream.fromFile(path);

        is.seek(7);

        check(is, 1, "23");
    }


    @Test
    public void testInputRGZTriesToReadFileTruncatedByOutputRGZ() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream o = RAGZOutputStream.toFile(path);
        o.close();
        o = RAGZOutputStream.toFile(path);
        RAGZInputStream i = RAGZInputStream.fromFile(path);
        assertEquals(0, i.available());
        o.close();
    }


    @Test
    public void testInputRgzTriesToReadFileTruncatedAndAppendedByOutputRgz() throws Exception {
        String path = tmpFile("test.gz");
        RAGZOutputStream o = RAGZOutputStream.toFile(path);
        o.close();

        RandomAccessFile f = new RandomAccessFile(path, "rw");
        o = new RAGZOutputStream(f, 4096);
        o.write("EFGH0n9482ch0897yhn98(*&BT987hn&*(^RTFGB&*JM907hnRFB67v*&GJMNH98&HB9V78ER76tu(jiohioNHYfvudBIJK:o(k_)ujh(rfgVTCRd7^".getBytes());
        f.close();

        RAGZInputStream is = RAGZInputStream.fromFile(path);
        is.close();
    }


    private void check(RAGZInputStream is, long pos, String str) throws Exception {
        byte[] b = new byte[str.length()];
        is.seek(pos);
        is.read(b);
        assertEquals("At position " + pos, str, new String(b, "UTF8"));
    }
}
