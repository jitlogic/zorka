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

package com.jitlogic.zorka.core.test.store;

import com.jitlogic.zorka.core.store.TraceDataFile;
import com.jitlogic.zorka.core.store.TraceDataStore;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TraceDataStoreUnitTest extends ZorkaFixture {

    private static final long HL = TraceDataFile.HEADER_LENGTH;

    TraceDataFile f1, f2;
    TraceDataStore s1, s2;


    @After
    public void tearDown() throws Exception {
        if (null != f1) f1.close();
        if (null != f2) f2.close();
        if (null != s1) s1.close();
        if (null != s2) s2.close();
    }


    @Test
    public void testCreateAndReopenEmptyChunkFile() throws Exception {
        String fname = zorka.path(getTmpDir(), "test.dat");
        f1 = new TraceDataFile(fname, 1, 1234);
        f2 = new TraceDataFile(fname, 1, 0);

        assertThat(new File(fname).exists()).isEqualTo(true);
        assertThat(new File(fname).length()).isEqualTo(HL);

        assertThat(f2.getStartPos()).isEqualTo(1234);
    }


    @Test
    public void testReadWriteDataOnOpenFile() throws Exception {
        String fname = zorka.path(getTmpDir(), "test.dat");

        f1 = new TraceDataFile(fname, 1, 0);
        f1.write("ABCD".getBytes()); f1.flush();

        f2 = new TraceDataFile(fname, 1, 0);

        assertThat(f2.getLogicalSize()).isEqualTo(4);
        assertThat(f2.getPhysicalSize()).isEqualTo(HL+4);

        assertThat(f2.read(0, 4)).isEqualTo("ABCD".getBytes());
        assertThat(f2.read(2, 2)).isEqualTo("CD".getBytes());
        assertThat(f2.read(2, 4)).isEqualTo("CD".getBytes());
    }


    @Test
    public void testCreateWriteRemoveReadFile() throws Exception {
        String fname = zorka.path(getTmpDir(), "test.dat");

        f1 = new TraceDataFile(fname, 1, 0);
        f1.write("ABCD".getBytes());

        f1.remove();

        assertThat(new File(fname).exists()).isEqualTo(false);
    }


    @Test
    public void testSortFiles() throws Exception {
        String fname = zorka.path(getTmpDir(), "test.dat");

        f1 = new TraceDataFile(fname, 2, 0);
        f2 = new TraceDataFile(fname, 1, 0);

        List<TraceDataFile> lst = Arrays.asList(f1, f2);
        Collections.sort(lst);

        assertThat(lst.get(0).getIndex()).isLessThan(lst.get(1).getIndex());
    }


    @Test
    public void testReopenAndWriteSomething() throws Exception {
        String fname = zorka.path(getTmpDir(), "test.dat");
        f1 = new TraceDataFile(fname, 1, 0); f1.write("ABCD".getBytes()); f1.close();
        f1 = new TraceDataFile(fname, 1, 0); f1.write("EFGH".getBytes()); f1.close();
        f2 = new TraceDataFile(fname, 1, 0);

        assertThat(f2.read(2, 4)).isEqualTo("CDEF".getBytes());
        assertThat(f2.getLogicalSize()).isEqualTo(8);
    }


    @Test
    public void testCreateWriteReadTrivialStore() throws Exception {
        s1 = new TraceDataStore(getTmpDir(), "test", "dat", 1024, 1024*1024);
        s1.write("ABCD".getBytes()); s1.flush();

        s2 = new TraceDataStore(getTmpDir(), "test", "dat", 1024, 1024*1024);

        File f = new File(getTmpDir() + File.separatorChar + "test_00000001.dat");

        assertThat(s2.read(0, 4)).isEqualTo("ABCD".getBytes());
        assertThat(s2.getEndPos()).isEqualTo(4);
        assertThat(s2.getPhysSize()).isEqualTo(HL + 4);
        assertThat(f.exists()).isTrue();
        assertThat(f.length()).isEqualTo(20);
    }

    @Test
    public void testSingleRotation() throws Exception {
        s1 = new TraceDataStore(getTmpDir(), "test", "dat", HL+4, 1024*1024);
        s1.write("ABCD".getBytes());
        s1.write("EFGH".getBytes());
        s1.flush();

        File f1 = new File(getTmpDir() + File.separatorChar + "test_00000001.dat");
        File f2 = new File(getTmpDir() + File.separatorChar + "test_00000002.dat");

        assertThat(f1.exists()).isTrue();
        assertThat(f1.length()).isEqualTo(HL+4);
        assertThat(f2.exists()).isTrue();
        assertThat(f2.length()).isEqualTo(HL+4);
    }


    @Test
    public void testReadAfterSingleRotation() throws Exception {
        s1 = new TraceDataStore(getTmpDir(), "test", "dat", HL+4, 1024*1024);
        s1.write("ABCD".getBytes());
        s1.write("EFGH".getBytes());
        s1.flush();

        s2 = new TraceDataStore(getTmpDir(), "test", "dat", HL+4, 1024*1024);
        assertThat(s2.read(0, 4)).isEqualTo("ABCD".getBytes());
        assertThat(s2.read(4, 4)).isEqualTo("EFGH".getBytes());
    }

    @Test
    public void testRotateAndRemoveOldFiles() throws Exception {
        s1 = new TraceDataStore(getTmpDir(), "test", "dat", HL+4, 4*(HL+4));
        s1.write("ABCD".getBytes());
        s1.write("EFGH".getBytes());
        s1.write("IJKM".getBytes());

        assertThat(s1.getStartPos()).isEqualTo(4);

        File f1 = new File(getTmpDir() + File.separatorChar + "test_00000001.dat");

        assertThat(f1.exists()).isFalse();
    }
}
