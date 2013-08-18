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

package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public class FileTraceOutput extends ZorkaAsyncThread<SymbolicRecord> implements TraceOutput {

    private static final ZorkaLog log = ZorkaLogger.getLog(FileTraceOutput.class);

    private File path;

    private int maxArchiveFiles;
    private long maxFileSize;
    private boolean compress;

    private TraceWriter traceWriter;

    private FileOutputStream fileStream;
    private OutputStream stream;

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
    protected void process(SymbolicRecord obj) {
        try {
            traceWriter.write(obj);
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

        for (int i = maxArchiveFiles-1; i > 0; i--) {
            f = new File(path.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(path.getPath() + "." + (i+1));
                f.renameTo(nf);
            }
        }

        path.renameTo(new File(path.getPath() + ".1"));

        log.info(ZorkaLogger.ZSP_SUBMIT, "Opening trace file: " + path);

        reopen();

        traceWriter.reset();
    }

    byte[] ZTRZ_MAGIC = new byte[] { 'Z', 'T', 'R', 'Z' };
    byte[] ZTRC_MAGIC = new byte[] { 'Z', 'T', 'R', 'C' };

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
    protected void open() {
        roll();
    }


    @Override
    protected void close() {
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
