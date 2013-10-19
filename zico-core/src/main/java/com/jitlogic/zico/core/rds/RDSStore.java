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
import java.util.*;
import java.util.regex.Pattern;

/**
 * Raw Data Store (RDS) builds upon RAGZ stream classes.
 */
public class RDSStore implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(RDSStore.class);

    private static Pattern RGZ_FILE = Pattern.compile("^[0-9a-f]{16}\\.rgz$");

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

    private List<RDSChunkFile> archivedFiles = new ArrayList<RDSChunkFile>();

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
                scanInputFile(fname);
                chunkOffsets.add(Long.parseLong(fname.substring(0, 16), 16));
            }
        }

        Collections.sort(archivedFiles);
        Collections.sort(chunkOffsets);

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


    private void scanInputFile(String fname) throws IOException {
        RDSChunkFile rcf = new RDSChunkFile(fname);
        archivedFiles.add(rcf);

        if (rcf.loffs + rcf.llen > logicalPos) {
            logicalPos = rcf.loffs + rcf.llen;
        }
    }


    private void reopen() throws IOException {
        RDSChunkFile rcf = archivedFiles.get(archivedFiles.size() - 1);

        if (output != null) {
            output.close();
            output = null;
        }

        if (input != null) {
            input.close();
            input = null;
        }

        output = new RAGZOutputStream(new RandomAccessFile(new File(basePath, rcf.fname), "rw"), segmentSize);
        outputStart = rcf.loffs;
        outputPos = rcf.loffs + rcf.llen;
        archivedFiles.remove(archivedFiles.size() - 1);
    }

    /**
     * (Re)opens latest output file.
     */
    private void rotate() throws IOException {
        String fname = String.format("%016x.rgz", logicalPos).toLowerCase();

        if (output != null) {
            output.close();
            archivedFiles.add(new RDSChunkFile(String.format("%016x.rgz", outputStart)));
            chunkOffsets.add(logicalPos);
        }

        if (input != null) {
            input.close();
            input = null;
        }

        output = new RAGZOutputStream(new RandomAccessFile(new File(basePath, fname), "rw"), segmentSize);
        outputStart = logicalPos;
        outputPos = 0;
    }


    public synchronized void cleanup() throws IOException {
        long size = output.physicalLength();

        for (RDSChunkFile f : archivedFiles) {
            size += f.plen;
        }

        while (size > maxSize && archivedFiles.size() > 0) {
            RDSChunkFile rcf = archivedFiles.get(0);
            File f = new File(basePath, rcf.fname);

            if (f.exists()) {
                f.delete();
            }

            size -= rcf.plen;

            archivedFiles.remove(0);

            chunkOffsets.remove(0);

            for (RDSCleanupListener listener : cleanupListeners) {
                listener.onChunkRemoved(rcf.loffs, rcf.llen);
            }

        }
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void close() throws IOException {
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

        output.write(data);

        long pos = logicalPos;

        logicalPos += data.length;
        outputPos += data.length;

        if (output.physicalLength() > fileSize) {
            rotate();
            cleanup();
        }

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

        if (archLOffs == -1 || offs < archLOffs || offs > archLOffs + archLLen) {

            archLOffs = -1;
            archLLen = 0;

            if (archInput != null) {
                archInput.close();
                archInput = null;
            }

            for (int i = 0; i < chunkOffsets.size(); i++) {
                if (offs >= chunkOffsets.get(i) && (i < chunkOffsets.size() - 1 || offs < chunkOffsets.get(i + 1))) {
                    RAGZInputStream ri = RAGZInputStream.fromFile(chunkFile(chunkOffsets.get(i)).getPath());
                    if (offs >= ri.logicalPos() && offs < ri.logicalPos() + ri.logicalLength()) {
                        archInput = ri;
                        archLOffs = ri.logicalPos();
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


    private class RDSChunkFile implements Comparable<RDSChunkFile> {
        private String fname;
        private long loffs = -1;
        private long llen = -1;
        private long plen = -1;

        public RDSChunkFile(String fname) throws IOException {
            RAGZInputStream is = null;
            this.fname = fname;
            try {
                loffs = Long.parseLong(fname.substring(0, 16), 16);
                File file = new File(basePath, fname);
                RAGZInputStream inp = RAGZInputStream.fromFile(file.getPath());
                llen = inp.logicalLength();
                plen = file.length();
                inp.close();
            } catch (Exception e) {
                log.error("Cannot open RDS chunk file '" + fname + "'", e);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }

        @Override
        public int compareTo(RDSChunkFile o) {
            long d = loffs - o.loffs;
            // Difference might be > 4G, so we can't just return it.
            return d == 0 ? 0 : (d > 0 ? 1 : -1);
        }

        @Override
        public String toString() {
            return fname;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RDSChunkFile && fname.equals(((RDSChunkFile) o).fname);
        }
    }
}
