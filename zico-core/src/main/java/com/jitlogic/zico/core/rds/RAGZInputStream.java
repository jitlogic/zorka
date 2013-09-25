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
package com.jitlogic.zico.core.rds;


import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class RAGZInputStream extends InputStream {

    // TODO CRC verification

    private List<RAGZSegment> segments;
    private RandomAccessFile input;

    private RAGZSegment curSegment;
    private byte[] logicalBuf;
    private long logicalPos;


    public static RAGZInputStream fromFile(String path) throws IOException {
        RandomAccessFile f = new RandomAccessFile(path, "r");
        return new RAGZInputStream(f);
    }


    public RAGZInputStream(RandomAccessFile input) throws IOException {
        this.input = input;
        segments = RAGZSegment.scan(input);
        if (segments.size() > 0) {
            RAGZSegment seg = segments.get(segments.size() - 1);
            if (!seg.isFinished()) {
                RAGZSegment.update(input, seg);
            }
        }
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

            if (curSegment == null || logicalPos >= curSegment.getLogicalPos() + curSegment.getLogicalLen()
                    || logicalPos < curSegment.getLogicalPos()) {
                curSegment = findSegment(logicalPos);
                if (curSegment == null) {
                    break;
                }
                logicalBuf = RAGZSegment.unpack(input, curSegment);
            }


            long cpos = logicalPos - curSegment.getLogicalPos();
            long csz = Math.min(llen, curSegment.getLogicalLen() - cpos);

            if (cpos + csz > logicalBuf.length) {
                logicalBuf = RAGZSegment.unpack(input, curSegment);
            }

            System.arraycopy(logicalBuf, (int) cpos, b, (int) lpos, (int) csz);

            logicalPos += csz;
            llen -= csz;
            lpos += csz;
        }

        return (int) (len - llen);
    }


    @Override
    public synchronized long skip(long n) throws IOException {
        return seek(logicalPos + n);
    }


    @Override
    public synchronized int available() throws IOException {
        if (curSegment != null) {
            return (int) (curSegment.getLogicalPos() + curSegment.getLogicalLen() - logicalPos);
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

        RAGZSegment seg = findSegment(lpos);

        if (seg != null) {
            seg = segments.get(segments.size() - 1);

        }

        if (seg != curSegment) {
            curSegment = seg;
            logicalBuf = RAGZSegment.unpack(input, seg);
        }

        if (lpos >= curSegment.getLogicalPos() + curSegment.getLogicalLen()) {
            lpos = curSegment.getLogicalPos() + curSegment.getLogicalLen();
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
     * @throws IOException
     */
    public synchronized long logicalLength() throws IOException {
        RAGZSegment seg = segments.get(segments.size() - 1);

        if (!seg.isFinished() && seg.getPhysicalPos() + seg.getPhysicalLen() < input.length()) {
            RAGZSegment.update(input, seg);
        }

        return seg.getLogicalPos() + seg.getLogicalLen();
    }


    public synchronized long physicalLength() throws IOException {
        return input.length();
    }


    private RAGZSegment findSegment(long logicalPos) throws IOException {
        for (RAGZSegment seg : segments) {
            if (logicalPos >= seg.getLogicalPos() && logicalPos < seg.getLogicalPos() + seg.getLogicalLen()) {
                return seg;
            } else if (!seg.isFinished()) {
                RAGZSegment.update(input, seg);
                if (logicalPos >= seg.getLogicalPos() && logicalPos < seg.getLogicalPos() + seg.getLogicalLen()) {
                    return seg;
                } else {
                    // This segment has been finished by concurrent process. Try scanning for more segments.
                    List<RAGZSegment> segs = RAGZSegment.scan(input,
                            seg.getLogicalPos() + seg.getLogicalLen(),
                            seg.getPhysicalPos() + seg.getPhysicalLen() + 8);
                    if (segs.size() > 0) {
                        segments.addAll(segs);
                        return findSegment(logicalPos);
                    } else {
                        return null;
                    }
                }
            }
        }

        return null;
    }


}
