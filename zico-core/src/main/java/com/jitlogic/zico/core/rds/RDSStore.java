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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Raw Data Store (RDS) builds upon RAGZ stream classes.
 */
public class RDSStore implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(RDSStore.class);

    public final static Pattern RGZ_FILE = Pattern.compile("^[0-9a-f]{16}\\.rgz$");

    private String basePath;
    private long maxSize;

    private long fileSize;
    private long segmentSize;

    private long logicalPos = 0;

    /**
     * Current input - associated with recently appended file.
     */
    private RAGZInputStream input;

    /**
     * Current output - new data is appended here.
     */
    private RAGZOutputStream output;

    /** */
    private long outputStart = 0, outputPos = 0;

    private List<Long> chunkOffsets = new ArrayList<Long>();

    private List<RDSCleanupListener> cleanupListeners = new ArrayList<RDSCleanupListener>();

    private RAGZInputStream archInput;
    private long archLOffs = -1, archLLen = 0;


    /**
     * @param basePath    path to directory containing store files
     * @param maxSize     maximum physical size of the store
     * @param fileSize    maximum physical size of a single file in the store
     * @param segmentSize segment size (inside single RAGZ file)
     * @throws IOException
     */
    public RDSStore(String basePath, long maxSize, long fileSize, long segmentSize,
                    RDSCleanupListener... cleanupListeners) throws IOException {

        this.basePath = basePath;
        this.maxSize = maxSize;
        this.fileSize = fileSize;
        this.segmentSize = segmentSize;

        this.cleanupListeners = Arrays.asList(cleanupListeners);

        open();
    }


    public String getBasePath() {
        return basePath;
    }


    private synchronized void open() throws IOException {
        File baseDir = new File(basePath);

        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new RDSException("Cannot create RDS directory: " + baseDir);
            }
        }

        if (!baseDir.isDirectory()) {
            throw new RDSException("RDS path: " + baseDir + " is not a directory !");
        }

        for (String fname : baseDir.list()) {
            File file = new File(baseDir, fname);
            if (RGZ_FILE.matcher(fname).matches() && file.isFile() && file.canRead()) {
                chunkOffsets.add(Long.parseLong(fname.substring(0, 16), 16));
            }
        }

        Collections.sort(chunkOffsets);

        if (chunkOffsets.size() > 0) {
            long o = chunkOffsets.get(chunkOffsets.size() - 1);
            RAGZInputStream in = RAGZInputStream.fromFile(chunkFile(o).getPath());
            logicalPos = o + in.logicalLength();
            in.close();
        }

        if (chunkOffsets.size() > 0 && chunkFile(chunkOffsets.get(chunkOffsets.size() - 1)).length() < fileSize) {
            try {
                reopen();
            } catch (Exception e) {
                rotate();
            }
        } else {
            rotate();
        }
    }

    private void reopen() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }

        if (input != null) {
            input.close();
            input = null;
        }

        outputStart = chunkOffsets.get(chunkOffsets.size() - 1);

        output = new RAGZOutputStream(new RandomAccessFile(chunkFile(outputStart), "rw"), segmentSize);
        outputPos = outputStart + output.logicalLength();
    }


    /**
     * (Re)opens latest output file.
     */
    public synchronized void rotate() throws IOException {
        String fname = String.format("%016x.rgz", logicalPos).toLowerCase();

        if (output != null) {
            output.close();
        }

        if (input != null) {
            input.close();
            input = null;
        }

        output = new RAGZOutputStream(new RandomAccessFile(new File(basePath, fname), "rw"), segmentSize);
        outputStart = logicalPos;
        outputPos = 0;

        chunkOffsets.add(outputStart);
    }


    public synchronized void cleanup() throws IOException {

        if (chunkOffsets.size() == 0) {
            return;
        }

        long size = 0;

        for (String fname : new File(basePath).list()) {
            size += new File(basePath, fname).length();
        }

        while (size > maxSize && chunkOffsets.size() > 0) {

            long chunkOffs = chunkOffsets.get(0);
            long chunkLen = (chunkOffsets.size() > 1 ? chunkOffsets.get(1) : outputStart) - chunkOffs;

            File f = chunkFile(chunkOffs);

            size -= f.length();

            if (f.exists()) {
                f.delete();
            }

            chunkOffsets.remove(0);

            for (RDSCleanupListener listener : cleanupListeners) {
                listener.onChunkRemoved(this, chunkOffs, chunkLen);
            }
        }
    }


    public synchronized void cleanup(long toOffs) throws IOException {
        while (chunkOffsets.size() > 1 && chunkOffsets.get(1) < toOffs) {
            long chunkOffs = chunkOffsets.get(0);
            long chunkLen = (chunkOffsets.size() > 1 ? chunkOffsets.get(1) : outputStart) - chunkOffs;

            File f = chunkFile(chunkOffs);

            if (f.exists()) {
                f.delete();
            }

            chunkOffsets.remove(0);

            for (RDSCleanupListener listener : cleanupListeners) {
                listener.onChunkRemoved(this, chunkOffs, chunkLen);
            }
        }
    }


    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }


    @Override
    public synchronized void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }

        if (input != null) {
            input.close();
            input = null;
        }

        if (archInput != null) {
            archInput.close();
            archInput = null;
        }
    }


    public long size() {
        return logicalPos;
    }


    public synchronized long write(byte[] data) throws IOException {

        if (output == null) {
            throw new RDSException("Trying to write data to closed RDS.");
        }

        if (data == null) {
            throw new RDSException("No data written.");
        }

        if (output.physicalLength() > fileSize) {
            rotate();
            cleanup();
        }

        output.write(data);

        long pos = logicalPos;

        logicalPos += data.length;
        outputPos += data.length;


        return pos;
    }


    public synchronized byte[] read(long offs, int length) throws IOException {

        if (offs >= outputStart && offs <= outputPos + outputStart) {
            if (input == null) {
                input = RAGZInputStream.fromFile(chunkFile(outputStart).getPath());
            }
            int len = length <= outputPos + outputStart - offs ? length : (int) (outputStart + outputPos - offs);
            byte[] data = new byte[len];
            input.seek(offs - outputStart);
            input.read(data);
            return data;
        }

        if (archLOffs == -1 || offs < archLOffs || offs >= archLOffs + archLLen) {

            archLOffs = -1;
            archLLen = 0;

            if (archInput != null) {
                archInput.close();
                archInput = null;
            }

            for (int i = 0; i < chunkOffsets.size() - 1; i++) {
                if (offs >= chunkOffsets.get(i) && offs < chunkOffsets.get(i + 1)) {
                    long base = chunkOffsets.get(i);
                    RAGZInputStream ri = RAGZInputStream.fromFile(chunkFile(chunkOffsets.get(i)).getPath());
                    if (offs >= base + ri.logicalPos() && offs < base + ri.logicalPos() + ri.logicalLength()) {
                        archInput = ri;
                        archLOffs = base + ri.logicalPos();
                        archLLen = ri.logicalLength();
                    } else {
                        ri.close();
                    }
                    break;
                }
            }
        }

        if (archLOffs != -1) {
            int len = length <= archLOffs + archLLen - offs
                    ? length : (int) (archLOffs + archLLen - offs);
            byte[] data = new byte[len];
            archInput.seek(offs - archLOffs);
            archInput.read(data);
            return data;
        }

        return new byte[0];
    }


    private File chunkFile(long offs) {
        return new File(basePath, String.format("%016x.rgz", offs));
    }

}
