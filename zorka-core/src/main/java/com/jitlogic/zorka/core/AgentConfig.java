/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core;


import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.integ.SyslogLib;
import com.jitlogic.zorka.core.integ.SyslogTrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.jitlogic.zorka.core.AgentConfigProps.*;

public class AgentConfig extends ZorkaConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/core/zorka.properties";


    public static boolean persistent = true;

    private String agentHome;

    private Map<String,String> agentAttrs = null;

    public AgentConfig(String home) {
        this.agentHome = home;
        reload();
    }

    public AgentConfig(Properties props) {
        properties = props;
        homeDir = get(ZORKA_HOME_DIR_PROP);
        loadIncludes();
        setBaseProps();
    }

    public void reload() {
        loadProperties(agentHome, "zorka.properties", DEFAULT_CONF_PATH);
        loadIncludes();
        setBaseProps();
    }

    private void loadIncludes() {
        for (String path : listCfg("include")) {
            loadCfg(properties, path, true);
        }
    }

    private void setBaseProps() {
        if (!properties.containsKey(SCRIPTS_DIR_PROP)) {
            properties.put(SCRIPTS_DIR_PROP, ZorkaUtil.path(homeDir, SCRIPTS_DIR_DVAL));
        }

        if (!properties.containsKey(ZORKA_LOG_DIR_PROP)) {
            properties.put(ZORKA_LOG_DIR_PROP, ZorkaUtil.path(homeDir, ZORKA_LOG_DIR_DVAL));
        }

        agentAttrs = mapCfg(ZORKA_AGENT_PROP, "host", stringCfg(ZORKA_HOSTNAME_PROP, System.getenv("HOSTNAME")));
    }

    public Map<String,String> getAgentAttrs() {
        return Collections.unmodifiableMap(agentAttrs);
    }

    /**
     * Adds and configures standard loggers.
     */
    public void initLoggers() {

        FileTrapper.ENABLE_FSYNC = boolCfg(ZORKA_LOG_FSYNC_PROP, ZORKA_LOG_FSYNC_DVAL);

        if (boolCfg(ZORKA_LOG_FILE_PROP, ZORKA_LOG_FILE_DVAL)) {
            initFileTrapper();
        }

        if (boolCfg(ZORKA_LOG_SYSLOG_PROP, ZORKA_LOG_SYSLOG_DVAL)) {
            initSyslogTrapper();
        }

        if (boolCfg(ZORKA_LOG_CONSOLE_PROP, ZORKA_LOG_CONSOLE_DVAL)) {
            initConsoleTrapper();
        }

        // Finish main logger configuration
        ZorkaLoggerFactory.getInstance().configure(this.getProperties());

        initNetkitLogger();

        log.info("Starting ZORKA agent " + get("zorka.version"));
    }


    private void initNetkitLogger() {

        Properties props = this.getProperties();

        // Configure netkit logging
        NetkitTrapperLoggerOutput netkitLogger = new NetkitTrapperLoggerOutput();
        StaticLoggerBinder.getSingleton().getLoggerFactory().swapInput(netkitLogger);

        List<String> levels = com.jitlogic.netkit.log.LoggerFactory.LEVELS;
        String level = props.getProperty("log.com.jitlogic.netkit", "INFO");

        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).equals(level)) {
                com.jitlogic.netkit.log.LoggerFactory.setLevel(i);
                break;
            }
        }

        com.jitlogic.netkit.log.LoggerFactory.setOutput(netkitLogger);
    }


    private void initConsoleTrapper() {
        ConsoleTrapper trapper = new ConsoleTrapper();

        ZorkaTrapper oldTrapper = ZorkaLoggerFactory.getInstance().swapTrapper(trapper);
        if (oldTrapper instanceof ZorkaService) {
            ((ZorkaService)oldTrapper).shutdown();
        }
    }

    /**
     * Creates and configures syslogt trapper according to configuration properties
     */
    private void initSyslogTrapper() {
        try {
            String server = stringCfg(ZORKA_LOG_SYSLOG_ADDR_PROP, ZORKA_LOG_SYSLOG_ADDR_DVAL);
            String hostname = stringCfg(ZORKA_HOSTNAME_PROP, ZORKA_HOSTNAME_DVAL);
            int syslogFacility = SyslogLib.getFacility(stringCfg(ZORKA_LOG_SYSLOG_FACILITY_PROP, ZORKA_LOG_SYSLOG_FACILITY_DVAL));

            SyslogTrapper trapper = new SyslogTrapper(server, hostname, syslogFacility, true);
            trapper.disableTrapCounter();
            trapper.start();

            ZorkaTrapper oldTrapper = ZorkaLoggerFactory.getInstance().swapTrapper(trapper);
            if (oldTrapper instanceof ZorkaService) {
                ((ZorkaService)oldTrapper).shutdown();
            }
        } catch (Exception e) {
            log.error("Error parsing logger arguments", e);
            log.info("Syslog trapper will be disabled.");
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }

    }


    /**
     * Creates and configures file trapper according to configuration properties
     */
    private void initFileTrapper() {
        String logDir = getLogDir();
        boolean logExceptions = boolCfg("zorka.log.exceptions", true);
        String logFileName = stringCfg(ZORKA_LOG_FILE_NAME_PROP, ZORKA_LOG_FILE_NAME_DVAL);
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4 * 1024 * 1024, maxLogs = 4; // TODO int -> long

        try {
            logThreshold = ZorkaLogLevel.valueOf(stringCfg(ZORKA_LOG_LEVEL_PROP, ZORKA_LOG_LEVEL_DVAL));
            maxSize = (int) (long) kiloCfg(ZORKA_LOG_FILE_SIZE_PROP, ZORKA_LOG_FILE_SIZE_DVAL);
            maxLogs = intCfg(ZORKA_LOG_FILE_NUM_PROP, ZORKA_LOG_FILE_NUM_DVAL);
        } catch (Exception e) {
            log.error("Error parsing logger arguments", e);
            log.info("File trapper will be disabled.");
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }


        FileTrapper trapper = FileTrapper.rolling(logThreshold,
                new File(logDir, logFileName).getPath(), maxLogs, maxSize, logExceptions);
        trapper.disableTrapCounter();
        trapper.start();

        ZorkaTrapper oldTrapper = ZorkaLoggerFactory.getInstance().swapTrapper(trapper);
        if (oldTrapper instanceof ZorkaService) {
            ((ZorkaService)oldTrapper).shutdown();
        }
    }

    public void writeCfg(String key, Object val) {
        super.writeCfg(key, val);
        if (persistent) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(agentHome, "zorka.properties"), true);
                String s = "\n" + key + " = " + val + "\n";
                fos.write(s.getBytes());
            } catch (IOException e) {
                log.error("I/O error when updating zorka.properties file.");
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        log.error("Error closing zorka.properties.", e);
                    }
                }
            }
        }
    }

    public static void setPersistent(boolean persistent) {
        AgentConfig.persistent = persistent;
    }

    @Override
    protected void markError(String msg, Throwable e) {
        super.markError(msg, e);
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
    }
}
