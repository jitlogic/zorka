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
package com.jitlogic.zorka.central;


import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;

/**
 * File backed trace entry set.
 */
public class TraceEntrySet implements Closeable {

    private DB db;
    private NavigableMap<Long,TraceEntry> tracesByOffs;


    public TraceEntrySet(String path) {
        db = DBMaker.newFileDB(new File(path)).make();
        tracesByOffs = db.getTreeMap("tracesByOffset");
    }


    @Override
    public void close() throws IOException {
        db.close();
    }


    public void save(TraceEntry entry) {
        tracesByOffs.put(entry.getOffs(), entry);
    }

}
