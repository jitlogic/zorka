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


import com.jitlogic.zorka.central.RDSStore;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

public class RawDataStoreUnitTest extends CentralFixture {

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
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        assertEquals("RDS size should increase after write.", 4, rds.size());
        rds.close();

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
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

    @Test @Ignore
    public void testWriteReadMultipleChunksMixedTx() throws Exception {
        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("First chunk should be written at offset 0", 0, rds.write("ABCD".getBytes()));
        rds.close();

        rds = new RDSStore(tmpFile("testrw"), 4096, 1024, 1024);
        assertEquals("Second chunk should be written at offset 4", 4, rds.write("EFGH".getBytes()));
        assertEquals("Thrid chunk should be written at offset 8", 8, rds.write("IJKL".getBytes()));
        rds.close();

        assertThat(rds.read(0, 8)).isEqualTo("ABCDEFGH".getBytes());
        //assertThat(rds.read(2, 4)).isEqualTo("CDEF".getBytes());
        //assertThat(rds.read(2, 8)).isEqualTo("CDEFGHIJ".getBytes());
        //assertThat(rds.read(6, 4)).isEqualTo("GHIJ".getBytes());
        //assertThat(rds.read(0, 12)).isEqualTo("ABCDEFGHIJKL".getBytes());
    }
}
