/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zico.core.ZicoUtil;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;


/**
 * This structure represents a single in-memory segment.
 */
public class RAGZSegment {


    /**
     * Physical position: where segment compressed data starts (so it excludes headers)
     */
    private long physicalPos;


    /**
     * Physical length: length of compressed data block
     */
    private long physicalLen;


    /**
     * Logical position: position in uncompressed data where this segment starts
     */
    private long logicalPos;


    /**
     * Logical length: length of uncompressed data stored in this segment
     */
    private long logicalLen;


    /**
     * If true, this segment has been finished and won't grow anymore. If false, this is the last segment where
     * new data is still being appended.
     */
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

    @Override
    public String toString() {
        return "RAGZSegment(ppos=" + physicalPos + ", plen=" + physicalLen + ", lpos=" + logicalPos + ", llen=" + logicalLen + ")";
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


    /**
     * Scans file for written segments.
     *
     * @param file       open file (to be scanned)
     * @param logicalPos logical position of first scanned segment
     * @param filePos    physical position where segments start (including GZ headers)
     * @return list of segments found
     * @throws IOException
     */
    public static List<RAGZSegment> scan(RandomAccessFile file, long logicalPos, long filePos) throws IOException {
        List<RAGZSegment> segments = new ArrayList<RAGZSegment>();

        file.seek(filePos);

        long lpos = logicalPos;

        while (file.getFilePointer() < file.length()) {
            byte m1 = file.readByte(), m2 = (byte) (file.readByte() & 0xff);
            if (m1 != 0x1f || m2 != (byte) 0x8b)
                throw new RAGZException(String.format("Invalid magic of gzip header: m1=0x%2x m2=0x%2x (pos=%d)",
                        m1, m2, file.getChannel().position() - 2));
            file.skipBytes(10);
            byte c1 = file.readByte(), c2 = file.readByte();
            if (c1 != 0x52 && c2 != 0x47)
                throw new RAGZException(String.format("Invalid magic of EXT header: c1=0x%2x c2=0x%2x (pos=%d)",
                        c1, c2, file.getChannel().position() - 2));
            file.skipBytes(2);
            long clen = ZicoUtil.readUInt(file);
            long cpos = file.getFilePointer();
            if (clen != 0 && file.length() >= file.getFilePointer() + clen + 8) {
                // Finished segment
                file.skipBytes((int) clen + 4);
                long llen = ZicoUtil.readUInt(file);
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


    /**
     * Updates segment information. Re-reads relevant segment information and updates relevant field in segment structure.
     *
     * @param file open random access file
     * @param seg  segment structure being updated
     * @throws IOException
     */
    public static void update(RandomAccessFile file, RAGZSegment seg) throws IOException {
        file.seek(seg.getPhysicalPos() - 4);
        long clen = ZicoUtil.readUInt(file);

        if (clen != 0 && file.length() >= file.getFilePointer() + clen + 8) {
            // Finished segment
            seg.setFinished(true);
            seg.setPhysicalLen(clen);
            file.seek(seg.getPhysicalPos() + seg.getPhysicalLen() + 4);
            seg.setLogicalLen(ZicoUtil.readUInt(file));
        } else {
            // Unfinished segment
            seg.setPhysicalLen(file.length() - seg.getPhysicalPos());
            seg.setLogicalLen(unpack(file, seg).length);

            // TODO search for next segment here
        }
    }


    /**
     * Uncompresses segment data.
     *
     * @param file open RAGZ file
     * @param seg  segment
     * @return
     * @throws IOException
     */
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
