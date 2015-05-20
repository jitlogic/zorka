/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.*;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


/**
 * Implements functionality of storing data generating by tracer to local files.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class FileTraceOutput extends ZorkaAsyncThread<SymbolicRecord> implements TraceStreamOutput {

    private static final ZorkaLog log = ZorkaLogger.getLog(FileTraceOutput.class);

    /**
     * Path to trace file.
     */
    private File path;

    /**
     * Maximum number of archived files
     */
    private int maxArchiveFiles;

    /**
     * Maximum file size
     */
    private long maxFileSize;

    /**
     * If true, output data will be compressed
     */
    private boolean compress;

    /**
     * Trace writer responsible for encoding output data
     */
    private TraceWriter traceWriter;

    /**
     * File output stream (physical file)
     */
    private FileOutputStream fileStream;

    /**
     * Logical output stream (possibly compressor stacked over file output stream)
     */
    private OutputStream stream;

    /**
     * Creates file output for tracer.
     *
     * @param traceWriter     trace writer
     * @param path            path to output file
     * @param maxArchiveFiles max number of archived files
     * @param maxFileSize     max file size
     * @param compress        enable compressions
     */
    public FileTraceOutput(TraceWriter traceWriter, File path, int maxArchiveFiles, long maxFileSize, boolean compress) {
        super("file-output");

        this.traceWriter = traceWriter;
        this.path = path;
        this.maxArchiveFiles = maxArchiveFiles;
        this.maxFileSize = maxFileSize;
        this.compress = compress;

        traceWriter.setOutput(this);
    }


    @Override
    public OutputStream getOutputStream() {
        return stream;
    }


    @Override
    protected void process(List<SymbolicRecord> objs) {
        try {
            for (SymbolicRecord obj : objs) {
                traceWriter.write(obj);
            }
            stream.flush();
            if (fileStream.getChannel().position() >= maxFileSize) {
                roll(); // TODO proper size limit control
                traceWriter.reset();
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZSP_SUBMIT, "Error writing trace data to file.", e);
            roll();
        }
    }


    /**
     * Rotates and reopens trace file.
     */
    private void roll() {

        if (stream != null) {
            close();
        }

        File f = new File(path.getPath() + "." + maxArchiveFiles);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxArchiveFiles - 1; i > 0; i--) {
            f = new File(path.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(path.getPath() + "." + (i + 1));
                f.renameTo(nf);
            }
        }

        path.renameTo(new File(path.getPath() + ".1"));

        log.info(ZorkaLogger.ZSP_SUBMIT, "Opening trace file: " + path);

        reopen();

        traceWriter.reset();
    }

    /**
     * Compressed trace file signature (magic bytes)
     */
    byte[] ZTRZ_MAGIC = new byte[]{'Z', 'T', 'R', 'Z'};

    /**
     * Uncompressed trace file signature (magic bytes)
     */
    byte[] ZTRC_MAGIC = new byte[]{'Z', 'T', 'R', 'C'};


    /**
     * Reopens trace file. This always creates new file.
     * Writes file signature just after reopen.
     */
    private void reopen() {
        try {
            fileStream = new FileOutputStream(path);

            if (compress) {
                log.info(ZorkaLogger.ZSP_SUBMIT, "Opening compressed trace file.");
                fileStream.write(ZTRZ_MAGIC);
                stream = new BufferedOutputStream(new DeflaterOutputStream(fileStream, new Deflater(6, true), 65536));

            } else {
                log.info(ZorkaLogger.ZSP_SUBMIT, "Opening plain trace file.");
                fileStream.write(ZTRC_MAGIC);
                stream = new BufferedOutputStream(fileStream);
            }

        } catch (FileNotFoundException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot open trace file " + path, e);
        } catch (IOException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot write to trace file " + path, e);
        }
    }


    @Override
    public void open() {
        log.info(ZorkaLogger.ZSP_CONFIG, "Starting file tracer output: " + path);
        roll();
    }


    @Override
    public void close() {
        log.info(ZorkaLogger.ZSP_CONFIG, "Stopping file tracer output: " + path);
        try {
            stream.close();
            stream = null;
        } catch (IOException e) {
            log.error(ZorkaLogger.ZSP_SUBMIT, "Error closing output stream.", e);
        }
    }


    @Override
    protected void flush() {
        try {
            stream.flush();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot flush trace file " + path, e);
        }
    }
}
