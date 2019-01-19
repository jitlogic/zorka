/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.stracer;

public class STraceBufManager {

    private STraceBufChunk chunks[];
    private int nChunks = 0;

    private int chunkSize;

    private int maxChunks;

    private int nGets, nPuts, nAllocs, nDrops;

    public STraceBufManager(int chunkSize, int maxChunks) {
        this.chunkSize = chunkSize;
        this.maxChunks = maxChunks;
        this.chunks = new STraceBufChunk[maxChunks];
    }

    public synchronized STraceBufChunk get() {
        nGets++;
        if (nChunks > 0) {
            STraceBufChunk ch = chunks[--nChunks];
            ch.reset();
            nAllocs++;
            return ch;
        }

        return new STraceBufChunk(chunkSize);
    }

    public synchronized void put(STraceBufChunk chunk) {
        nPuts++;
        for (STraceBufChunk ch = chunk; ch != null; ch = ch.getNext()) {
            if (nChunks < maxChunks) {
                chunks[nChunks++] = ch;
            } else {
                nDrops++;
            }
        }
    }

    public synchronized int getNChunks() {
        return nChunks;
    }

    public synchronized int getNGets() {
        return nGets;
    }

    public synchronized int getNputs() {
        return nPuts;
    }

    public synchronized int getnAllocs() {
        return nAllocs;
    }

    public synchronized int getnDrops() {
        return nDrops;
    }
}
