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
package com.jitlogic.zorka.central.rds;


import com.jitlogic.zorka.central.CentralUtil;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class RAGZSegment {

    private long physicalPos;
    private long physicalLen;
    private long logicalPos;
    private long logicalLen;
    private boolean finished;


    public RAGZSegment(long physicalPos, long physicalLen, long logicalPos, long logicalLen, boolean finished) {
        this.physicalPos = physicalPos;
        this.physicalLen = physicalLen;
        this.logicalPos = logicalPos;
        this.logicalLen = logicalLen;
        this.finished = finished;
    }


    public long getPhysicalPos() {
        return physicalPos;
    }


    public void setPhysicalPos(long physicalPos) {
        this.physicalPos = physicalPos;
    }


    public long getPhysicalLen() {
        return physicalLen;
    }


    public void setPhysicalLen(long physicalLen) {
        this.physicalLen = physicalLen;
    }


    public long getLogicalPos() {
        return logicalPos;
    }


    public void setLogicalPos(long logicalPos) {
        this.logicalPos = logicalPos;
    }


    public long getLogicalLen() {
        return logicalLen;
    }


    public void setLogicalLen(long logicalLen) {
        this.logicalLen = logicalLen;
    }


    public boolean isFinished() {
        return finished;
    }


    public void setFinished(boolean finished) {
        this.finished = finished;
    }


    private static long defaultLogicalLen = 1048576 * 2;


    public static List<RAGZSegment> scan(RandomAccessFile file) throws IOException {
        return scan(file, 0, 0);
    }


    public static List<RAGZSegment> scan(RandomAccessFile file, long logicalPos, long physicalPos) throws IOException {
        List<RAGZSegment> segments = new ArrayList<RAGZSegment>();

        file.seek(physicalPos);

        long lpos = logicalPos;

        while (file.getFilePointer() < file.length()) {
            byte m1 = file.readByte(), m2 = (byte) (file.readByte() & 0xff);
            if (m1 != 0x1f || m2 != (byte) 0x8b)
                throw new RAGZException(String.format("Invalid magic of gzip header: m1=0x%2x m2=0x%2x", m1, m2));
            file.skipBytes(10);
            byte c1 = file.readByte(), c2 = file.readByte();
            if (c1 != 0x52 && c2 != 0x47)
                throw new RAGZException(String.format("Invalid magic of EXT header: c1=0x%2x c2=0x%2x", c1, c2));
            file.skipBytes(2);
            long clen = CentralUtil.readUInt(file);
            long cpos = file.getFilePointer();
            if (clen != 0) {
                // Finished segment
                file.skipBytes((int) clen + 4);
                long llen = CentralUtil.readUInt(file);
                segments.add(new RAGZSegment(cpos, clen, lpos, llen, true));
                lpos += llen;
            } else {
                // Unfinished segment (last one)
                RAGZSegment seg = new RAGZSegment(cpos, file.length() - cpos, lpos, 0, false);
                segments.add(seg);
                break; // presumeably unfinished segment is the last one
            }
        } // while

        return segments;
    }


    public static void update(RandomAccessFile file, RAGZSegment seg) throws IOException {
        file.seek(seg.getPhysicalPos() - 4);
        long clen = CentralUtil.readUInt(file);

        if (clen != 0) {
            // Finished segment
            seg.setFinished(true);
            seg.setPhysicalLen(clen);
            file.seek(seg.getPhysicalPos() + seg.getPhysicalLen() + 4);
            seg.setLogicalLen(CentralUtil.readUInt(file));
        } else {
            // Unfinished segment
            seg.setPhysicalLen(file.length() - seg.getPhysicalPos());
            seg.setLogicalLen(unpack(file, seg).length);

            // TODO search for next segment here
        }
    }


    public static byte[] unpack(RandomAccessFile file, RAGZSegment seg) throws IOException {
        byte[] ibuf = new byte[(int) seg.getPhysicalLen()];

        if (ibuf.length == 0) {
            return ibuf;
        }

        file.seek(seg.getPhysicalPos());

        if (file.read(ibuf) < ibuf.length) {
            throw new RAGZException("Unexpected end of file.");
        }

        byte[] obuf = new byte[(int) (seg.getLogicalLen() > 0 ? seg.getLogicalLen() : defaultLogicalLen)];

        try {
            Inflater inf = new Inflater(true);
            inf.setInput(ibuf);
            int n = 0;
            while (inf.getRemaining() > 0) {
                // TODO this is inefficient - avoid reallocations, use common buffer in some way etc.
                int x = inf.inflate(obuf, n, obuf.length - n);
                if (x == 0) {
                    break;  // TODO check how exactly inflater is working and fix this method accordingly
                }
                n += x;
                if (n == obuf.length && inf.getRemaining() > 0) {
                    obuf = ZorkaUtil.clipArray(obuf, obuf.length + 4096);
                }
            }
            if (n > 0) {
                if (n != obuf.length) {
                    obuf = ZorkaUtil.clipArray(obuf, n);
                }
                return obuf;
            } else {
                return new byte[0];
            }
        } catch (DataFormatException e) {
            throw new RAGZException("Problem uncompressing data.", e);
        }
    }

}
