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

import com.jitlogic.zorka.central.db.DbContext;
import com.jitlogic.zorka.central.db.DbRecord;
import com.jitlogic.zorka.central.db.HostTable;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class StoreManager implements Closeable, ZicoDataProcessorFactory {

    private static ZorkaLog log = ZorkaLogger.getLog(StoreManager.class);

    private String dataDir;

    private CentralConfig config;

    private DbContext dbContext;

    private SymbolRegistry symbolRegistry;

    private HostTable hostTable;

    private Map<String,Store> stores = new HashMap<String, Store>();


    public StoreManager(CentralConfig config, DbContext dbContext, SymbolRegistry symbolRegistry, HostTable hostTable) {
        this.config = config;
        this.symbolRegistry = symbolRegistry;
        this.hostTable = hostTable;
        this.dbContext = dbContext;
        this.dataDir = config.stringCfg("central.data.dir", null);
    }

    // TODO use ID as proper host ID as get(id), rename this method to reflect it is get-or-create method, not a simple getter
    public synchronized Store get(String hostname) {
        if (!stores.containsKey(hostname)) {
            DbRecord host = hostTable.getHost(hostname, null);
            if (host != null) {
                Store store = new Store(config, host, dataDir, dbContext, symbolRegistry);
                stores.put(hostname, store);
            }
        }
        return stores.get(hostname);
    }


    @Override
    public synchronized void close() throws IOException {
        for (Map.Entry<String,Store> entry : stores.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot close agent store for host " + entry.getKey(), e);
            }
        }
    }


    @Override
    public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {
        return new ReceiverContext(get(hello.getHostname()));
    }
}
