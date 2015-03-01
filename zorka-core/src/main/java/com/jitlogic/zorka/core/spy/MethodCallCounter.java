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
package com.jitlogic.zorka.core.spy;


import com.jitlogic.zorka.common.tracedata.MethodCallCounterRecord;

import java.util.ArrayList;
import java.util.List;

public class MethodCallCounter {

    private static final int INITIAL_SIZE = 64;

    private long keys[], vals[];

    private long initialTime = 0L;

    private int numEntries = 0, maxEntries = 0;

    private static final long FREE = 0;
    private static final long MASK = 0x1fffffL;

    public MethodCallCounter() {
        keys = new long[INITIAL_SIZE];
        vals = new long[INITIAL_SIZE];
        numEntries = 0;
        maxEntries = INITIAL_SIZE * 3 / 4;
    }


    public void logCall(int classId, int methodId, int signatureId, long tstamp) {
        logCalls(classId, methodId, signatureId, tstamp, 1);
    }


    public void logCalls(int classId, int methodId, int signatureId, long tstamp, long nCalls) {
        long key = classId | (((long) methodId) << 21) | (((long) signatureId) << 42);

        if (initialTime == tstamp) {
            initialTime = tstamp;
        }

        if (numEntries > maxEntries) {
            rehash(maxEntries * 2);
        }

        int idx = index(key);

        if (keys[idx] == FREE) {
            keys[idx] = key;
            vals[idx] = nCalls;
            numEntries++;
        } else {
            vals[idx] += nCalls;
        }
    }


    public List<MethodCallCounterRecord> getRecords() {
        List<MethodCallCounterRecord> ret = new ArrayList<MethodCallCounterRecord>();

        for (int i = 0; i < keys.length; i++) {
            long key = keys[i];
            if (key != FREE) {
                ret.add(new MethodCallCounterRecord((int) (key & MASK), (int) ((key >> 21) & MASK), (int) ((key >> 42) & MASK), vals[i]));
            }
        }

        return ret;
    }


    public void sum(MethodCallCounter mcc) {
        long[] mkeys = mcc.keys;
        long[] mvals = mcc.vals;
        long tst = mcc.initialTime;

        for (int i = 0; i < mkeys.length; i++) {
            long key = mkeys[i];
            if (key != FREE) {
                logCalls((int) (key & MASK), (int) ((key >> 21) & MASK), (int) ((key >> 42) & MASK), tst, mvals[i]);
            }
        }
    }


    public void clear() {
        initialTime = 0L;
        numEntries = 0;
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 0;
        }
    }


    private int lhash(long l) {
        int v = (int) (l ^ (l >> 32));
        return v >= 0 ? v : -v;
    }


    private void rehash(int capacity) {
        int olen = keys.length;

        long[] okeys = keys;
        long[] ovals = vals;

        long[] nkeys = new long[capacity];
        long[] nvals = new long[capacity];

        keys = nkeys;
        vals = nvals;
        maxEntries = capacity * 3 / 4;

        for (int i = 0; i < olen; i++) {
            long key = okeys[i];
            int idx = index(key);
            nkeys[idx] = key;
            nvals[idx] = ovals[i];
        }
    }


    private int index(long key) {
        long[] keys = this.keys;
        int length = keys.length;

        int hash = lhash(key);
        int i = hash % length;

        if (i < 0) {
            System.out.println("kurwa!");
        }

        while (keys[i] != FREE && keys[i] != key) {
            i = (i > 0) ? i - 1 : length - 1;
        }

        return i;
    }
}
