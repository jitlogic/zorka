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
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.rds.RDSCleanupListener;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zico.test.support.ZicoFixture;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

public class RDSStoreUnitTest extends ZicoFixture {

    RDSStore rds;


    @After
    public void tearDown() throws Exception {
        if (rds != null) {
            rds.close();
            rds = null;
        }
    }


    @Test
    public void testOpenCloseEmptyDataStore() throws Exception {
        String baseDir = tmpFile("testdata");
        rds = new RDSStore(baseDir, 4096, 1024, 1024);

        assertEquals("RDS size should return 0", 0, rds.size());
        assertTrue("RDS base directory should exist.", new File(baseDir).exists());

        File file = new File(baseDir, "0000000000000000.rgz");

        assertTrue("First RDS file should exist.", file.exists());

        rds.close();

        assertTrue("First RDS file should contain GZ header after close. ", file.length() > 0);
    }


    @Test
    public void testTrivialWriteRead() throws Exception {
        String tmpf = tmpFile("testrw");
        rds = new RDSStore(tmpf, 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        assertEquals("RDS size should increase after write.", 4, rds.size());
        rds.close();

        rds = new RDSStore(tmpf, 4096, 1024, 1024);
        assertEquals("RDS size should be non-zero after reopen.", 4, rds.size());
        assertThat(rds.read(0, 4)).describedAs("Should return encoded 'ABCD' string.").isEqualTo("ABCD".getBytes());
        rds.close();
    }


    @Test
    public void testWriteReadTwoChunks() throws Exception {
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        assertEquals("First chunk should be written at offset 0", 4, rds.write("EFGH".getBytes()));
        assertEquals("RDS size should increase after write.", 8, rds.size());
        rds.close();

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("RDS size should be non-zero after reopen.", 8, rds.size());
        assertThat(rds.read(0, 8)).describedAs("Should return 'ABCDEFGH' string.").isEqualTo("ABCDEFGH".getBytes());
        assertThat(rds.read(2, 4)).describedAs("Should return 'CDEF' string.").isEqualTo("CDEF".getBytes());
        rds.close();
    }


    @Test
    public void testWriteReadInOneTx() throws Exception {
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        assertThat(rds.read(0, 4)).describedAs("Should return 'ABCD' string.").isEqualTo("ABCD".getBytes());
        rds.close();
    }

    @Test
    public void testWriteReadMultipleChunksInOneTx() throws Exception {
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        assertEquals("Second chunk should be written at offset 4", 4, rds.write("EFGH".getBytes()));
        assertEquals("Thrid chunk should be written at offset 8", 8, rds.write("IJKL".getBytes()));
        assertThat(rds.read(0, 8)).isEqualTo("ABCDEFGH".getBytes());
        assertThat(rds.read(2, 4)).isEqualTo("CDEF".getBytes());
        assertThat(rds.read(2, 8)).isEqualTo("CDEFGHIJ".getBytes());
        assertThat(rds.read(6, 4)).isEqualTo("GHIJ".getBytes());
        assertThat(rds.read(0, 12)).isEqualTo("ABCDEFGHIJKL".getBytes());
        rds.close();
    }

    @Test
    public void testWriteReadMultipleChunksMixedTx() throws Exception {
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        rds.close();

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("Second chunk should be written at offset 4", 4, rds.write("EFGH".getBytes()));
        assertEquals("Thrid chunk should be written at offset 8", 8, rds.write("IJKL".getBytes()));
        rds.close();
    }

    @Test
    public void testOpenWriteReopenAndCheckIfThereIsNoSecondFile() throws Exception {

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        rds.write("ABCD".getBytes());
        rds.close();

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        rds.write("EFGH".getBytes());
        rds.close();

        assertEquals(1, new File(tmpFile("testrw")).list().length);

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertThat(rds.read(0, 8)).isEqualTo("ABCDEFGH".getBytes());
        rds.close();
    }


    private void verifyChunks(String path, Long... ref) {
        List<Long> chunks = new ArrayList<Long>();
        for (String fname : new File(path).list()) {
            if (RDSStore.RGZ_FILE.matcher(fname).matches()) {
                chunks.add(Long.parseLong(fname.substring(0, 16), 16));
            }
        }
        Collections.sort(chunks);

        assertEquals(Arrays.<Long>asList(ref), chunks);
    }


    @Test
    public void testFileRotationInsideRdsSingleSeg() throws Exception {
        byte[] r0 = new byte[0], r1 = rand(1100), r2 = rand(1100), r3 = rand(1100), r4 = rand(1100), r5 = rand(1100);
        String path = tmpFile("testrw");
        rds = new RDSStore(path, 4096, 1024, 1024);

        verifyChunks(path, 0L);

        rds.write(r1);
        verifyChunks(path, 0L, 1100L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);

        rds.write(r2);
        verifyChunks(path, 0L, 1100L, 2200L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);

        rds.write(r3);
        verifyChunks(path, 0L, 1100L, 2200L, 3300L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);

        rds.write(r4);
        verifyChunks(path, 1100L, 2200L, 3300L, 4400L);
        assertThat(rds.read(0, 1100)).isEqualTo(r0);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);
        assertThat(rds.read(3300, 1100)).isEqualTo(r4);

        rds.write(r5);
        verifyChunks(path, 2200L, 3300L, 4400L, 5500L);
        assertThat(rds.read(0, 1100)).isEqualTo(r0);
        assertThat(rds.read(1100, 1100)).isEqualTo(r0);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);
        assertThat(rds.read(3300, 1100)).isEqualTo(r4);
        assertThat(rds.read(4400, 1100)).isEqualTo(r5);
        assertThat(rds.read(5500, 1100)).isEqualTo(r0);

        rds.close();

    }


    @Test
    public void testFileRotationInsideRdsDoubleSeg() throws Exception {
        byte[] r0 = new byte[0], r1 = rand(1100), r2 = rand(1100), r3 = rand(1100), r4 = rand(1100),
                r5 = rand(1100), r6 = rand(1100);

        String path = tmpFile("testrw");
        rds = new RDSStore(path, 6000, 2048, 1024);

        rds.write(r1);
        verifyChunks(path, 0L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r0);

        rds.write(r2);
        verifyChunks(path, 0L, 2200L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);

        rds.write(r3);
        verifyChunks(path, 0L, 2200L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);

        rds.write(r4);
        verifyChunks(path, 0L, 2200L, 4400L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);
        assertThat(rds.read(3300, 1100)).isEqualTo(r4);

        rds.write(r5);
        verifyChunks(path, 0L, 2200L, 4400L);
        assertThat(rds.read(0, 1100)).isEqualTo(r1);
        assertThat(rds.read(1100, 1100)).isEqualTo(r2);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);
        assertThat(rds.read(3300, 1100)).isEqualTo(r4);
        assertThat(rds.read(4400, 1100)).isEqualTo(r5);

        rds.write(r6);
        verifyChunks(path, 2200L, 4400L, 6600L);
        assertThat(rds.read(0, 1100)).isEqualTo(r0);
        assertThat(rds.read(1100, 1100)).isEqualTo(r0);
        assertThat(rds.read(2200, 1100)).isEqualTo(r3);
        assertThat(rds.read(3300, 1100)).isEqualTo(r4);
        assertThat(rds.read(4400, 1100)).isEqualTo(r5);
        assertThat(rds.read(5500, 1100)).isEqualTo(r6);
        assertThat(rds.read(6600, 1100)).isEqualTo(r0);
    }

}
