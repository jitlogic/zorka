package com.jitlogic.zorka.core.spy.stracer;

import com.jitlogic.zorka.common.cbor.CborInput;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChunkedCborInput extends CborInput {

    /** Current chunk index */
    private int ccidx = 0;

    /** Current chunk byte buffer */
    private byte[] buf;

    /** Position in current chunk byte buffer */
    private int pos;

    /** Limit in current chunk byte buffer */
    private int lim;

    private int size = 0, len;

    private List<STraceBufChunk> chunks = new ArrayList<STraceBufChunk>();

    public ChunkedCborInput(STraceBufChunk chunk) {
        for (STraceBufChunk c = chunk; c != null; c = c.getNext()) {
            this.chunks.add(c);
        }

        Collections.sort(chunks,
                new Comparator<STraceBufChunk>() {
                    @Override
                    public int compare(STraceBufChunk o1, STraceBufChunk o2) {
                        return o1.getExtOffset() - o2.getExtOffset();
                    }
                });

        for (int i = 1; i < chunks.size(); i++) {
            if (chunks.get(i).getExtOffset() != chunks.get(i-1).getExtOffset()+chunks.get(i-1).getPosition()) {
                throw new ZorkaRuntimeException("Chunks list should be contiguous and from single trace.");
            }
        }

        reset();
    }

    private void nextChunk() {
        if (ccidx < chunks.size()) {
            STraceBufChunk c = chunks.get(ccidx++);
            buf = c.getBuffer();
            pos = c.getStartOffset();
            lim = c.getPosition();
        } else {
            buf = null;
            pos = lim = 0;
        }
    }

    @Override
    public byte peekB() {
        if (pos >= lim) nextChunk();
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        return buf[pos];
    }

    @Override
    public byte readB() {
        if (pos >= lim) nextChunk();
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        size--;
        return buf[pos++];
    }

    @Override
    public byte[] readB(int len) {
        if (len > size) throw new ZorkaRuntimeException("Unexpected EOD");
        byte[] rslt = new byte[len];
        int rpos = 0;
        while (rpos < len) {
            if (pos >= lim) nextChunk();
            if (pos < lim && size > 0) {
                int cl = Math.min(len-rpos, lim-pos);
                System.arraycopy(buf, pos, rslt, rpos, cl);
                pos += cl; size -= cl; rpos += cl;
            } else {
                throw new ZorkaRuntimeException("Unexpected EOD");
            }
        }
        return rslt;
    }

    @Override
    public int readI() {
        size--;
        if (pos >= lim) nextChunk();
        return pos < lim ? buf[pos++] & 0xff : -1;
    }

    @Override
    public long readL() {
        size--;
        if (pos >= lim) nextChunk();
        return pos < lim ? buf[pos++] & 0xffL : -1;
    }

    @Override
    public int size() {
        return Math.max(size, 0);
    }

    public boolean eof() {
        return size <= 0;
    }

    @Override
    public int position() {
        return len-size;
    }

    @Override
    public void position(int pos) {
        throw new ZorkaRuntimeException("Internal error: ChunkedCborInput.position(int) not implemented TBD");
    }

    public void reset() {
        ccidx = 0;
        size = 0;

        for (STraceBufChunk c : chunks) {
            size += c.size();
        }

        len = size;

        nextChunk();
    }
}
