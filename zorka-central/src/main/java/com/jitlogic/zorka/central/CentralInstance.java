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


import com.jitlogic.zorka.common.ZorkaAgent;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.zico.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

public class CentralInstance {

    private final static Logger log = LoggerFactory.getLogger(CentralInstance.class);


    private CentralConfig config;

    private HostStoreManager storeManager;

    private ZicoService zicoService;

    private BasicDataSource ds;

    private SymbolRegistry symbolRegistry;

    private TraceTemplateManager templater;

    private TraceCache traceCache;

    public CentralInstance(CentralConfig config) {
        this.config = config;
    }


    public synchronized void start() {

        initLoggers(config.getProperties());

        if (config.boolCfg("zico.service", true)) {
            getZicoService().start();
        }

    }


    public synchronized void stop() {

        if (zicoService != null) {
            zicoService.stop();
        }

        if (storeManager != null) {
            try {
                storeManager.close();
            } catch (IOException e) {
                // TODO log error
            }
        }

        if (ds != null) {
            try {
                ds.close();
            } catch (Exception e) {
                // TODO log error
            }
        }

    }


    /**
     * Adds and configures standard loggers.
     *
     * @param props configuration properties
     */
    public void initLoggers(Properties props) {
        // TODO ZorkaLogger and ZorkaLog will be replaced with slf4j+custom backend (logging to standard trapper)
        //ZorkaLogger.configure(props);
    }


    public CentralConfig getConfig() {
        return config;
    }


    public synchronized SymbolRegistry getSymbolRegistry() {
        if (symbolRegistry == null) {
            symbolRegistry = new DbSymbolRegistry(getDs());
        }

        return symbolRegistry;
    }


    public synchronized TraceCache getTraceCache() {
        if (null == traceCache) {
            traceCache = new TraceCache(getConfig().intCfg("trace.cache.size", 5));
        }
        return traceCache;
    }


    public synchronized HostStoreManager getStoreManager() {
        if (null == storeManager) {
            storeManager = new HostStoreManager(getConfig(), getDs(), getSymbolRegistry(), getTraceCache(), getTemplater());
        }
        return storeManager;
    }


    public synchronized ZicoService getZicoService() {
        if (null == zicoService) {
            zicoService = new ZicoService(
                    config.stringCfg("central.listen.addr", "0.0.0.0"),
                    config.intCfg("central.listen.port", ZicoService.COLLECTOR_PORT),
                    getStoreManager());
        }
        return zicoService;
    }


    public synchronized BasicDataSource getDs() {

        if (ds == null) {
            ds = new BasicDataSource();
            ds.setDriverClassName(config.stringCfg("central.db.driver", null));
            ds.setUrl(config.stringCfg("central.db.url", null));
            ds.setUsername(config.stringCfg("central.db.user", null));
            ds.setPassword(config.stringCfg("central.db.pass", null));

            if (config.boolCfg("central.db.create", false)) {
                new JdbcTemplate(ds).execute("RUNSCRIPT FROM 'classpath:/com/jitlogic/zorka/central/"
                        + config.stringCfg("central.db.type", "h2") + ".createdb.sql'");
            }

        }

        return ds;
    }


    public synchronized TraceTemplateManager getTemplater() {
        if (templater == null) {
            templater = new TraceTemplateManager(getDs(), getSymbolRegistry());
        }

        return templater;
    }

    private static final long MB = 1024 * 1024;

    /**
     * Reference to self-monitoring agent
     */
    private ZorkaAgent agent;

    public void setAgent(ZorkaAgent agent) {
        this.agent = agent;
    }

    public List<String> systemInfo() {
        List<String> info = new ArrayList<String>();

        // TODO use agent to present these things - it's already there :)

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        MemoryUsage hmu = mem.getHeapMemoryUsage();

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long ss = uptime % 60, mm = ((uptime - ss) / 60) % 60, hh = ((uptime - mm * 60 - ss) / 3600) % 60,
                dd = ((uptime - hh * 3600 - mm * 60 - ss) / 86400);

        info.add("Uptime: " + String.format("%dd %02d:%02d:%02d", dd, hh, mm, ss));

        info.add("Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                hmu.getUsed() / MB, hmu.getMax() / MB, 100.0 * hmu.getUsed() / hmu.getMax()));

        MemoryUsage nmu = mem.getNonHeapMemoryUsage();

        info.add("Non-Heap Memory: " + String.format("%dMB/%dMB (%.1f%%)",
                nmu.getUsed() / MB, nmu.getMax() / MB, 100.0 * nmu.getUsed() / nmu.getMax()));

        try {
            if (agent != null) {
                info.addAll(Arrays.asList(agent.query("central.info()").split("\n")));
            }
        } catch (Exception e) {
            log.warn("Call to self-monitoring agent failed.", e);
        }

        return info;
    }
}
