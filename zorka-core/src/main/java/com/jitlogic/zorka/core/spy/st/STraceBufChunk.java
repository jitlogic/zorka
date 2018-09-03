/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.st;

import com.jitlogic.zorka.common.tracedata.MetadataChecker;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;

import java.io.IOException;

public class STraceBufChunk implements SymbolicRecord {

    private byte[] buffer;

    private int offset;

    private int size;

    private STraceBufChunk next;

    public STraceBufChunk(int size) {
        this.size = size;
        this.buffer = new byte[size];
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public STraceBufChunk getNext() {
        return next;
    }

    public void setNext(STraceBufChunk next) {
        this.next = next;
    }

    public void reset() {
        offset = 0;
        size = 0;
        next = null;
    }

    @Override
    public void traverse(MetadataChecker checker) throws IOException {

    }
}


