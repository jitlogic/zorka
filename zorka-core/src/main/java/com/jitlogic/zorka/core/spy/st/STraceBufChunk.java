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

/**
 * Stores chunk of trace data as it is recorded by tracer.
 */
public class STraceBufChunk implements SymbolicRecord {

    /** Trace data buffer. */
    private byte[] buffer;

    /** Internal offset: number of bytes to skip at the beginning of current buffer. */
    private int startOffset;

    /** External offset: offset of current chunk in whole trace */
    private int extOffset;

    /** Size of this chunk (in bytes). */
    private int position;

    /** Low word of top-level trace UUID (sent to collector in order to bind all chunks together). */
    private long uuidL;

    /** High word of top-level trace UUID (sent to collector in order to bind all chunks together). */
    private long uuidH;

    /** Previous chunk (used if chunks are grouped together). */
    private STraceBufChunk next;

    public STraceBufChunk(int position) {
        this.position = position;
        this.buffer = new byte[position];
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getExtOffset() {
        return extOffset;
    }

    public void setExtOffset(int extOffset) {
        this.extOffset = extOffset;
    }

    public int getPosition() {
        return position;
    }

    public int size() {
        return position - startOffset;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public STraceBufChunk getNext() {
        return next;
    }

    public void setNext(STraceBufChunk next) {
        this.next = next;
    }

    public void reset() {
        extOffset = 0;
        position = 0;
        uuidL = 0L;
        uuidH = 0L;
        next = null;
    }

    public long getUuidL() {
        return uuidL;
    }

    public void setUuidL(long uuidL) {
        this.uuidL = uuidL;
    }

    public long getUuidH() {
        return uuidH;
    }

    public void setUuidH(long uuidH) {
        this.uuidH = uuidH;
    }

    @Override
    public void traverse(MetadataChecker checker) throws IOException {

    }
}


