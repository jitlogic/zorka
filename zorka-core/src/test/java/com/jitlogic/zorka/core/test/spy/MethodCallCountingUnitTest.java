/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.tracedata.MethodCallCounterRecord;
import com.jitlogic.zorka.core.spy.MethodCallCounter;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


public class MethodCallCountingUnitTest extends ZorkaFixture {

    @Test
    public void testMethodCallCounterLoggingCalls() throws Exception {
        Random r = new Random();

        List<MethodCallCounterRecord> recs = new ArrayList<MethodCallCounterRecord>();
        List<Integer> counters = new ArrayList<Integer>();

        // Generate some initial data
        for (int i = 0; i < 10000; i++) {

            MethodCallCounterRecord rec = new MethodCallCounterRecord(
                    r.nextInt(1500000) + 1, r.nextInt(1500000) + 2, r.nextInt(1500000) + 3, r.nextInt(10) + 1);

            recs.add(rec);
            counters.add(0);
        }

        MethodCallCounter mcc = new MethodCallCounter();

        // Fill in method call counter
        while (true) {
            int incs = 0;

            for (int i = 0; i < recs.size(); i++) {
                MethodCallCounterRecord rec = recs.get(i);
                if (counters.get(i) < rec.getnCalls()) {
                    mcc.logCall(rec.getClassId(), rec.getMethodId(), rec.getSignatureId(), 10L);
                    counters.set(i, counters.get(i) + 1);
                    incs++;
                }
            }

            if (incs == 0) {
                break;
            }
        }

        Set<MethodCallCounterRecord> results = new HashSet<MethodCallCounterRecord>();
        results.addAll(mcc.getRecords());

        // Indicate (potential) errors
        for (MethodCallCounterRecord mcr : recs) {
            if (!results.contains(mcr)) {
                MethodCallCounterRecord mcrr = null;
                for (MethodCallCounterRecord mcr2 : results) {
                    if (mcr2.getClassId() == mcr.getClassId() && mcr2.getMethodId() == mcr.getMethodId()
                            && mcr2.getSignatureId() == mcr.getSignatureId()) {
                        mcrr = mcr2;
                        break;
                    }
                }
                System.out.println("Results mismatch: " + mcr + " <-> " + mcrr);
            }
        }

        // Check results correctness
        assertEquals("Number of registered records should be the same.", recs.size(), results.size());

        for (MethodCallCounterRecord mcr : recs) {
            assertTrue("Result should contain record " + mcr, results.contains(mcr));
        }
    }

}
