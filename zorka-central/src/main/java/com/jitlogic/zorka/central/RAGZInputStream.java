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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RAGZInputStream extends InputStream {

    // TODO CRC verification

    private static class Segment {
        private long physicalPos;
        private long physicalLen;
        private long logicalPos;
        private long logicalLen;
        private boolean finished;

        public Segment(long physicalPos, long physicalLen, long logicalPos, long logicalLen, boolean finished) {
            this.physicalPos = physicalPos;
            this.physicalLen = physicalLen;
            this.logicalPos = logicalPos;
            this.logicalLen = logicalLen;
            this.finished = finished;
        }
    }

    private List<Segment> segments;
    private RandomAccessFile input;

    private Segment curSegment;
    private byte[] logicalBuf;
    private long logicalPos;

    private long defaultLogicalLen = 1048576 * 2;


    public static RAGZInputStream fromFile(String path) throws IOException {
        RandomAccessFile f = new RandomAccessFile(path, "r");
        return new RAGZInputStream(f);
    }


    public RAGZInputStream(RandomAccessFile input) throws IOException {
        this.input = input;
        scanSegments();
    }


    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];

        if (read(b, 0, 1) < 1) {
            return -1;
        }

        return b[0] & 0xff;
    }


    @Override
    public synchronized int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("Null buffer for read");
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        long lpos = off, llen = len;

        // TODO clip llen if needed


        while (llen > 0) {

            if (curSegment == null || logicalPos >= curSegment.logicalPos+curSegment.logicalLen || logicalPos < curSegment.logicalPos) {
                curSegment = findSegment(logicalPos);
                if (curSegment == null) {
                    break;
                }
                logicalBuf = unpackSegment(curSegment);
            }


            long cpos = logicalPos - curSegment.logicalPos;
            long csz = min(llen, curSegment.logicalLen - cpos);
            System.arraycopy(logicalBuf, (int) cpos, b, (int)lpos, (int)csz);

            logicalPos += csz;
            llen -= csz; lpos += csz;
        }

        return (int)(len - llen);
    }


    private long min(long l1, long l2) {
        return l1 < l2 ? l1 : l2;
    }


    @Override
    public synchronized long skip(long n) throws IOException {
        return seek(logicalPos + n);
    }


    @Override
    public synchronized int available() throws IOException {
        if (curSegment != null) {
            return (int)(curSegment.logicalPos  + curSegment.logicalLen - logicalPos);
        } else {
            return 0;
        }
    }


    @Override
    public void close() throws IOException {
        input.close();
    }


    public synchronized long seek(long pos) throws IOException {
        long lpos = pos;

        if (lpos < 0) {
            lpos = 0;
        }

        if (lpos == logicalPos) {
            return 0;
        }

        Segment seg = findSegment(lpos);

        if (seg != null) {
            seg = segments.get(segments.size()-1);

        }

        if (seg != curSegment) {
            curSegment = seg;
            logicalBuf = unpackSegment(seg);
        }

        if (lpos >= curSegment.logicalPos + curSegment.logicalLen) {
            lpos = curSegment.logicalPos + curSegment.logicalLen;
        }

        long offs = lpos - logicalPos;

        logicalPos = lpos;

        return offs;
    }


    public synchronized long logicalPos() throws IOException {
        return logicalPos;
    }


    /**
     * Returns logical length of compressed data.
     *
     * @return
     *
     * @throws IOException
     */
    public synchronized long logicalLength() throws IOException {
        Segment seg = segments.get(segments.size()-1);

        if (!seg.finished && seg.physicalPos + seg.physicalLen < input.length()) {
            updateSegment(seg);
        }

        return seg.logicalPos + seg.logicalLen;
    }


    private void updateSegment(Segment seg) throws IOException {
        input.seek(seg.physicalPos-4);
        int clen = input.readInt();

        if (clen != 0) {
            // Finished segment
            seg.finished = true;
            seg.physicalLen = clen;
            input.seek(seg.physicalPos + seg.physicalLen + 4);
            seg.logicalLen = readUInt();
        } else {
            // Unfinished segment
            seg.physicalLen = input.length() - seg.physicalPos;
            seg.logicalLen = unpackSegment(seg).length;

            // TODO search for next segment here
        }
    }


    public synchronized long physicalLength() throws IOException {
        return input.length();
    }


    private Segment findSegment(long logicalPos) throws IOException {
        for (Segment seg : segments) {
            if (logicalPos >= seg.logicalPos && logicalPos < seg.logicalPos+seg.logicalLen) {
                return seg;
            } else if (!seg.finished) {
                updateSegment(seg);
                if (logicalPos >= seg.logicalPos && logicalPos < seg.logicalPos+seg.logicalLen) {
                    return seg;
                }
            }
        }

        return null;
    }


    private void scanSegments() throws IOException {
        segments = new ArrayList<Segment>();

        input.seek(0);

        long lpos = 0;

        while (input.getFilePointer() < input.length()) {
            long fp = input.getFilePointer(), lp = input.length();
            byte m1 = input.readByte(), m2 = (byte)(input.readByte() & 0xff);
            if (m1 != 0x1f || m2 != (byte)0x8b)
                throw new RAGZException(String.format("Invalid magic of gzip header: m1=0x%2x m2=0x%2x", m1, m2));
            input.skipBytes(10);
            byte c1 = input.readByte(), c2 = input.readByte();
            if (c1 != 0x52 && c2 != 0x47)
                throw new RAGZException(String.format("Invalid magic of EXT header: c1=0x%2x c2=0x%2x", c1, c2));
            input.skipBytes(2);
            long clen = readUInt();
            long cpos = input.getFilePointer();
            if (clen != 0) {
                // Finished segment
                input.skipBytes((int)clen+4);
                long llen = readUInt();
                segments.add(new Segment(cpos, clen, lpos, llen, true));
                lpos += llen;
            } else {
                // Unfinished segment (last one)
                Segment seg = new Segment(cpos, clen, lpos, 0, false);
                updateSegment(seg);
                segments.add(seg);
                break; // presumeably unfinished segment is the last one
            }
        } // while
    }


    private long readUInt() throws IOException {
        byte[] b = new byte[4];

        if (input.read(b) != 4) {
            throw new EOFException("EOF encountered when reading UINT");
        }

        return CentralUtil.toUIntBE(b);
    }



    private byte[] unpackSegment(Segment seg) throws IOException {
        byte[] ibuf = new byte[(int)seg.physicalLen];

        if (ibuf.length == 0) {
            return ibuf;
        }

        input.seek(seg.physicalPos);

        if (input.read(ibuf) < ibuf.length) {
            throw new RAGZException("Unexpected end of file.");
        }

        byte[] obuf = new byte[(int)(seg.logicalLen > 0 ? seg.logicalLen : defaultLogicalLen)];

        try {
            Inflater inf = new Inflater(true);
            inf.setInput(ibuf);
            int n = inf.inflate(obuf);
            if (n > 0) {
                if (n != obuf.length) {
                    obuf = ZorkaUtil.clipArray(obuf, n);
                }
                return obuf;
            } else {
                return new byte[0];
            }
        } catch (DataFormatException e) {
            throw new RAGZException("Problem uncompressing input data.", e);
        }
    }
}
