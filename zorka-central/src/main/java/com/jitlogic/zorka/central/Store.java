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


import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Represents performance data store for a single agent.
 *
 */
public class Store implements Closeable {

    private final static ZorkaLog log = ZorkaLogger.getLog(Store.class);

    private String hostname;
    private String rootPath;
    private CentralConfig config;

    private RDSStore rds;
    private SymbolSet symbols;
    private TraceEntrySet traces;


    public Store(StoreManager manager, CentralConfig config, String hostname, String rootPath) {
        this.config = config;
        this.hostname = hostname;
        this.rootPath = rootPath;

        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
    }


    public String getHostname() {
        return hostname;
    }


    public String toString() {
        return "central.Store(" + hostname + ")";
    }


    public RDSStore getRds() {
        if (rds == null) {
            String rdspath = ZorkaUtil.path(rootPath, "traceadata");
            try {
                rds = new RDSStore(rdspath,
                    config.kiloCfg("rds.file.size", 16*1024*1024L).intValue(),
                    config.kiloCfg("rds.max.size", 256*1024*1024L),
                    config.kiloCfg("rds.seg.size", 1024*1024L));
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_STORE, "Cannot open RDS store at '" + rdspath + "'", e);
            }
        }
        return rds;
    }


    public synchronized SymbolSet getSymbols() {
        if (symbols == null) {
            symbols = new SymbolSet(ZorkaUtil.path(rootPath, "symbols.db"));
        }
        return symbols;
    }


    public synchronized TraceEntrySet getTraces() {
        if (traces == null) {
            traces = new TraceEntrySet(ZorkaUtil.path(rootPath, "traces.db"));
        }
        return traces;
    }


    @Override
    public synchronized void close() throws IOException {
        try {
            if (symbols != null) {
                symbols.close();
                symbols = null;
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_STORE, "Cannot close symbols store '" + symbols + "' for " + hostname , e);
        }

        try {
            if(traces != null) {
                traces.close();
                traces = null;
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_STORE, "Cannot close trace index '" + traces + "' for " + hostname, e);
        }

        try {
            if (rds != null) {
                rds.close();
                rds = null;
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_STORE, "Cannot close RDS store '" + rds + "' for " + hostname, e);
        }
    }
}
