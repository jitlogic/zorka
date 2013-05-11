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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChunkedDataStore implements Closeable {

    private String path, prefix, suffix;
    private long maxFileSize, maxPhysSize;
    private List<ChunkedDataFile> files = new ArrayList<ChunkedDataFile>();

    private Pattern fpattern;

    public ChunkedDataStore(String path, String prefix, String suffix, long maxFileSize, long maxPhysSize) throws IOException {
        this.path = path;
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxFileSize = maxFileSize;
        this.maxPhysSize = maxPhysSize;
        this.fpattern = Pattern.compile("^" + prefix + "_([0-9]+)\\." + suffix);

        scan();
    }


    public synchronized long getStartPos() {
        return files.get(0).getStartPos();
    }


    public synchronized long getEndPos() {
        ChunkedDataFile f = files.get(files.size()-1);
        return f.getStartPos()+f.getLogicalSize();
    }

    public synchronized long getPhysSize() {
        long size = 0;

        for (ChunkedDataFile f : files) {
            size += f.getPhysicalSize();
        }

        return size;
    }

    // TODO writes cannot span over many files

    public synchronized long write(byte[] chunk) throws IOException {
        ChunkedDataFile file = files.get(files.size() - 1);
        long pos = file.write(chunk);

        if (file.getPhysicalSize() >= maxFileSize) {
            rotate();
        }

        return pos;
    }

    // TODO reads cannot span over many files

    public synchronized byte[] read(long pos, long len) throws IOException {
        long startPos = getStartPos(), endPos = getEndPos();

        if (pos < startPos || pos >= endPos) {
            throw new IOException("Position " + pos + " is outside of (" + startPos + "-" + endPos + ") scope.");
        }

        int idx;
        ChunkedDataFile file = null;

        for (idx = 0; idx < files.size(); idx++) {
            ChunkedDataFile f = files.get(idx);
            if (pos >= f.getStartPos() && pos < f.getStartPos()+f.getLogicalSize()) {
                file = f;
                break;
            }
        }

        return file.read(pos, len);
    }


    public synchronized void scan() throws IOException {
        File dir = new File(path);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory '" + path + "'");
            }
        }

        if (!dir.isDirectory()) {
            throw new IOException("Not a directory: '" + path + "'");
        }

        for (File f : dir.listFiles()) {
            Matcher m = fpattern.matcher(f.getName());
            if (m.matches() && f.isFile() && f.canRead()) {
                int idx = Integer.parseInt(m.group(1));
                files.add(new ChunkedDataFile(f.getPath(), idx, 0L));
            }
        }

        if (files.size() == 0) {
            String fname = String.format("%s_%08d.%s", prefix, 1, suffix);
            files.add(new ChunkedDataFile(path + File.separatorChar + fname, 1, 0));
        } else {
            Collections.sort(files);
        }
    }


    public synchronized void flush() throws IOException {
        files.get(files.size()-1).flush();
    }



    private void rotate() throws IOException {
        ChunkedDataFile lastf = files.get(files.size()-1);
        String fname = String.format("%s_%08d.%s", prefix, lastf.getIndex()+1, suffix);
        lastf.close();
        files.add(new ChunkedDataFile(path + File.separatorChar + fname, lastf.getIndex() + 1, getEndPos()));

        long size = getPhysSize();

        while (size > maxPhysSize-maxFileSize && files.size() > 1) {
            ChunkedDataFile f = files.get(0);
            size -= f.getPhysicalSize();
            f.close();
            f.remove();
            files.remove(0);
        }

    }


    @Override
    public void close() throws IOException {
        for (ChunkedDataFile f : files) {
            f.close();
        }
    }

}
