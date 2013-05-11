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

package com.jitlogic.zorka.core.store;

import com.jitlogic.zorka.core.util.ByteBuffer;

import java.io.*;

public class ChunkedDataFile implements Closeable, Comparable<ChunkedDataFile> {

    public static final int HEADER_LENGTH = 16;

    private String fname;
    private int index;
    private long startPos;
    private long size;

    private FileOutputStream output;


    public ChunkedDataFile(String fname, int index, long startPos) throws IOException {
        this.fname = fname;
        this.index = index;
        this.startPos = startPos;
        readHeader();
    }


    private void readHeader() throws IOException {
        File f = new File(fname);


        if (f.length() > 0) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(fname);
                byte[] b = new byte[HEADER_LENGTH];
                is.read(b); is.close(); is = null;
                ByteBuffer buf = new ByteBuffer(b);
                startPos = buf.getLong();
            } catch (IOException e) {
                if (is != null) { try { is.close(); } catch (IOException e1) { } }
                throw e;
            }
            size = f.length() - HEADER_LENGTH;
        } else {
            open();
            writeHeader();
        }
    }


    private void writeHeader() throws IOException {
        byte[] b = new byte[HEADER_LENGTH];
        ByteBuffer buf = new ByteBuffer(b);
        buf.putLong(startPos);
        output.write(b);
        output.flush();
        size = 0;
    }


    public String getPath() {
        return fname;
    }


    public int getIndex() {
        return index;
    }


    public long getStartPos() {
        return startPos;
    }


    public long getPhysicalSize() {
        return size + HEADER_LENGTH;
    }


    public long getLogicalSize() {
        return size;
    }


    public long write(byte[] chunk) throws IOException {
        open();
        long pos = startPos+size;
        output.write(chunk);
        size += chunk.length;
        return pos;
    }


    public byte[] read(long pos, long len) throws IOException {
        FileInputStream is = null;

        try {
            long lpos = pos - startPos;
            long llen = size-lpos > len ? len : size-lpos;
            is = new FileInputStream(fname);
            is.getChannel().position(lpos + HEADER_LENGTH);
            byte[] b = new byte[(int)llen];
            is.read(b); is.close(); is = null;
            return b;
        } catch (IOException e) {
            if (is != null) try { is.close(); } catch (Exception e1) { }
            throw e;
        }
    }


    public void flush() throws IOException {
        if (output != null) {
            output.flush();
        }
    }


    private void open() throws IOException {
        if (output == null) {
            output = new FileOutputStream(fname, true);
        }
    }


    @Override
    public void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
    }


    public void remove() throws IOException {
        close();
        File f = new File(fname);
        f.delete();
    }


    @Override
    public int compareTo(ChunkedDataFile o) {
        return this.index - o.index;
    }
}
