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


import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.FileTrapper;
import com.jitlogic.zorka.common.util.ZorkaLogLevel;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.zico.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class CentralInstance {

    private final static Logger log = LoggerFactory.getLogger(CentralInstance.class);


    private CentralConfig config;

    private HostStoreManager storeManager;

    private ZicoService zicoService;

    private BasicDataSource ds;

    private SymbolRegistry symbolRegistry;

    private TraceTemplater traceTemplater;

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
        if (config.boolCfg("central.filelog", true)) {
            initFileTrapper();
        }

        // TODO ZorkaLogger and ZorkaLog will be replaced with slf4j+custom backend (logging to standard trapper)
        // TODO ?? or the other way around ??

        //if (config.boolCfg("zorka.syslog", false)) {
        //    initSyslogTrapper();
        //}

        ZorkaLogger.configure(props);
    }


    /**
     * Creates and configures file trapper according to configuration properties
     */
    private void initFileTrapper() {
        String logDir = config.getLogDir();
        boolean logExceptions = config.boolCfg("zorka.log.exceptions", true);
        String logFileName = config.stringCfg("zorka.log.fname", "zorka.log");
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4 * 1024 * 1024, maxLogs = 4; // TODO int -> long

        try {
            logThreshold = ZorkaLogLevel.valueOf(config.stringCfg("zorka.log.level", "INFO"));
            maxSize = (int) (long) config.kiloCfg("zorka.log.size", 4L * 1024 * 1024);
            maxLogs = config.intCfg("zorka.log.num", 8);
        } catch (Exception e) {
            log.error("Error parsing logger arguments", e);
            log.info("File trapper will be disabled.");
            // TODO AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }


        FileTrapper trapper = FileTrapper.rolling(logThreshold,
                new File(logDir, logFileName).getPath(), maxLogs, maxSize, logExceptions);
        trapper.disableTrapCounter();
        trapper.start();

        ZorkaLogger.getLogger().addTrapper(trapper);
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
            storeManager = new HostStoreManager(getConfig(), getDs(), getSymbolRegistry(), getTraceCache(), getTraceTemplater());
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


    public synchronized TraceTemplater getTraceTemplater() {
        if (traceTemplater == null) {
            traceTemplater = new TraceTemplater(getDs(), getSymbolRegistry());
        }

        return traceTemplater;
    }
}
