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

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RDSStoreStressTest extends ZicoFixture {

    RDSStore rds;

    public final static int CYCLES = 10000;
    public final static long RDS_SIZE = 1 * 1048576;
    public final static long RDS_FILE = 64 * 1024;
    public final static long RDS_SEGMENT = 16 * 1024;

    @After
    public void tearDown() throws Exception {
        if (rds != null) {
            rds.close();
            rds = null;
        }
    }


    @Test
    public void testRdsMiniStress() throws Exception {
        final List<byte[]> chunks = new ArrayList<byte[]>();
        final List<Long> positions = new ArrayList<Long>();

        RDSCleanupListener cleaner = new RDSCleanupListener() {
            @Override
            public void onChunkRemoved(Long start, Long length) {
                while (positions.size() > 0 && positions.get(0) < start + length) {
                    positions.remove(0);
                    chunks.remove(0);
                }
            }
        };

        String path = tmpFile("testrw");
        rds = new RDSStore(path, RDS_SIZE, RDS_FILE, RDS_SEGMENT, cleaner);

        for (int cyc = 0; cyc < CYCLES; cyc++) {
            byte[] chunk = new byte[rand.nextInt(1024) + 128];
            rand.nextBytes(chunk);
            chunks.add(chunk);
            positions.add(rds.write(chunk));

            if (chunks.size() > 10) {
                for (int i = 0; i < 5; i++) {
                    int n = rand.nextInt(chunks.size());
                    byte[] ref = chunks.get(n);
                    long pos = positions.get(n);
                    assertThat(rds.read(pos, ref.length))
                            .describedAs("Chunk at position [" + pos + "," + ref.length + "]")
                            .isEqualTo(ref);
                }
            }

        }
    }

}
