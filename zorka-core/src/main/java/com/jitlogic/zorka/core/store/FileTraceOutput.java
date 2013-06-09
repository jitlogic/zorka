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

import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.io.*;


public class FileTraceOutput extends ZorkaAsyncThread<Submittable> implements TraceOutput {

    private static final ZorkaLog log = ZorkaLogger.getLog(FileTraceOutput.class);

    private File path;

    private int maxArchiveFiles;
    private long maxFileSize;

    private TraceWriter traceWriter;

    private FileOutputStream fileStream;
    private OutputStream stream;

    public FileTraceOutput(TraceWriter traceWriter, File path, int maxArchiveFiles, long maxFileSize) {
        super("file-output");

        this.traceWriter = traceWriter;
        this.path = path;
        this.maxArchiveFiles = maxArchiveFiles;
        this.maxFileSize = maxFileSize;

        traceWriter.setOutput(this);
    }


    @Override
    public OutputStream getOutputStream() {
        return stream;
    }


    @Override
    protected void process(Submittable obj) {
        try {
            traceWriter.write(obj);
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

        try {
            fileStream = new FileOutputStream(path);
            stream = new BufferedOutputStream(fileStream);
        } catch (FileNotFoundException e) {
            log.error(ZorkaLogger.ZTR_ERRORS, "Cannot open trace file " + path, e);
        }

        traceWriter.reset();
    }


    @Override
    protected void open() {
        log.info(ZorkaLogger.ZSP_SUBMIT, "Kurwa: " + path);
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
