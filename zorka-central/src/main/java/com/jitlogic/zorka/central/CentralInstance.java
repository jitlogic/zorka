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
import com.jitlogic.zorka.central.db.DbSymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.FileTrapper;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogLevel;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.zico.ZicoService;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class CentralInstance {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private CentralConfig config;

    private StoreManager storeManager;

    private ZicoService zicoService;

    private DbContext db;

    private SymbolRegistry symbolRegistry;


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

        if (db != null) {
            try {
                db.close();
            } catch (IOException e) {
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

        // TODO
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Error parsing logger arguments", e);
            log.info(ZorkaLogger.ZAG_ERRORS, "File trapper will be disabled.");
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
            symbolRegistry = new DbSymbolRegistry(getDb().getJdbcTemplate());
        }

        return symbolRegistry;
    }


    public synchronized StoreManager getStoreManager() {
        if (null == storeManager) {
            storeManager = new StoreManager(getConfig(), getDb(), getSymbolRegistry());
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


    public synchronized DbContext getDb() {
        if (db == null) {
            db = new DbContext(getConfig());
        }
        return db;
    }
}
