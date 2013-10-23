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
package com.jitlogic.zico.core;

import com.jitlogic.zico.data.HostInfo;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Singleton
public class HostStoreManager implements Closeable, ZicoDataProcessorFactory, RowMapper<HostStore> {

    private final static Logger log = LoggerFactory.getLogger(HostStoreManager.class);

    private String dataDir;

    private ZicoConfig config;
    private SymbolRegistry symbolRegistry;

    private TraceTypeRegistry traceTypeRegistry;
    private TraceTemplateManager templater;
    private TraceCache cache;

    private boolean enableSecurity;

    private Map<Integer, HostStore> storesById = new HashMap<Integer, HostStore>();
    private Map<String, HostStore> storesByName = new HashMap<String, HostStore>();

    private JdbcTemplate jdbc;
    private DataSource ds;

    @Inject
    public HostStoreManager(ZicoConfig config, DataSource ds, SymbolRegistry symbolRegistry,
                            TraceCache cache, TraceTypeRegistry traceTypeRegistry, TraceTemplateManager templater) {
        this.config = config;
        this.symbolRegistry = symbolRegistry;
        this.dataDir = config.stringCfg("zico.data.dir", null);
        this.ds = ds;
        this.cache = cache;
        this.traceTypeRegistry = traceTypeRegistry;
        this.templater = templater;

        this.jdbc = new JdbcTemplate(ds);
        this.enableSecurity = config.boolCfg("zico.security", false);
    }


    public synchronized HostStore getOrCreateHost(String hostName, String hostAddr) {

        if (storesByName.containsKey(hostName)) {
            return storesByName.get(hostName);
        }

        List<HostStore> lst = jdbc.query("select * from HOSTS where HOST_NAME = ?", this, hostName);

        if (lst.size() == 0) {
            jdbc.update("insert into HOSTS (HOST_NAME,HOST_ADDR,HOST_PATH) values (?,?,?)",
                    hostName, hostAddr, safePath(hostName));
            return getOrCreateHost(hostName, hostAddr);
        } else {
            return lst.get(0);
        }
    }


    private String safePath(String hostname) {
        return hostname;
    }


    public synchronized HostStore getHost(int hostId) {
        if (storesById.containsKey(hostId)) {
            return storesById.get(hostId);
        }
        return jdbc.queryForObject("select * from HOSTS where HOST_ID = ?", this, hostId);
    }


    // TODO use ID as proper host ID as get(id), rename this method to reflect it is get-or-create method, not a simple getter
    // TODO this method is redundant
    public synchronized HostStore get(String hostName, boolean create) {
        if (!storesByName.containsKey(hostName) && create) {
            HostStore host = getOrCreateHost(hostName, null);
            if (host != null) {
                storesByName.put(hostName, host);
            }
        }

        return storesByName.get(hostName);
    }


    @Override
    public synchronized void close() throws IOException {
        for (Map.Entry<String, HostStore> entry : storesByName.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                log.error("Cannot close agent store for host " + entry.getKey(), e);
            }
        }
    }


    @Override
    // TODO move this outside this class
    public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {
        HostStore store = get(hello.getHostname(), !enableSecurity);

        if (store == null) {
            throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
        }

        if (store.getHostInfo().getAddr() == null) {
            store.getHostInfo().setAddr(socket.getInetAddress().getHostAddress());
            store.save();
        }

        if (enableSecurity) {
            HostInfo hi = store.getHostInfo();

            if (hi.getAddr() != null && !hi.getAddr().equals("" + socket.getInetAddress())) {
                throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
            }

            if (hi.getPass() != null && !hi.getPass().equals(hello.getAuth())) {
                throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Unauthorized.");
            }
        }

        return new ReceiverContext(ds, store, traceTypeRegistry);
    }

    @Override
    public synchronized HostStore mapRow(ResultSet rs, int rowNum) throws SQLException {
        int hostId = rs.getInt("HOST_ID");

        if (storesById.containsKey(hostId)) {
            HostStore store = storesById.get(hostId);
            store.updateInfo(rs);
            return store;
        } else {
            HostStore store = new HostStore(this, cache, rs);
            storesById.put(store.getHostInfo().getId(), store);
            return store;
        }
    }

    public List<HostStore> list() {
        return jdbc.query("select * from HOSTS order by HOST_NAME", this);
    }

    public synchronized void delete(int hostId) throws IOException {
        // TODO this might potentially take a long time and block other processes - do only necessary parts in synchronized section
        HostStore store = getHost(hostId);
        if (store != null) {
            String rootPath = store.getRootPath();
            store.close();
            ZorkaUtil.rmrf(rootPath);
            jdbc.update("delete from TRACES where HOST_ID = ?", hostId);
            jdbc.update("delete from HOSTS where HOST_ID = ?", hostId);
            storesById.remove(store.getHostInfo().getId());
            storesByName.remove(store.getHostInfo().getName());
        }

    }

    public ZicoConfig getConfig() {
        return config;
    }

    public SymbolRegistry getSymbolRegistry() {
        return symbolRegistry;
    }

    public String getDataDir() {
        return dataDir;
    }

    public JdbcTemplate getJdbc() {
        return jdbc;
    }

    public DataSource getDs() {
        return ds;
    }

    public TraceTemplateManager getTemplater() {
        return templater;
    }
}
