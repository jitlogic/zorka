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
import org.slf4j.impl.ZorkaLogLevel;
import org.slf4j.impl.ZorkaLoggerFactory;
import org.slf4j.impl.ZorkaTrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class AgentConfig extends ZorkaConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/core/zorka.properties";
    public static final String PROP_SCRIPTS_DIR = "zorka.scripts.dir";

    public static final String PROP_PROFILE_DIR = "zorka.profile.dir";

    public static boolean persistent = true;

    private String agentHome;

    private Map<String,String> agentAttrs = null;

    public AgentConfig(String home) {
        this.agentHome = home;
        reload();
    }

    public void reload() {
        loadProperties(agentHome, "zorka.properties", DEFAULT_CONF_PATH);
        setBaseProps();
    }


    public AgentConfig(Properties props) {
        properties = props;
        homeDir = get(PROP_HOME_DIR);
        setBaseProps();
    }


    private void setBaseProps() {
        if (!properties.containsKey(PROP_SCRIPTS_DIR)) {
            properties.put(PROP_SCRIPTS_DIR, ZorkaUtil.path(homeDir, "scripts"));
        }

        if (!properties.containsKey(PROP_PROFILE_DIR)) {
            properties.put(PROP_PROFILE_DIR, ZorkaUtil.path(homeDir, "profiles"));
        }

        if (!properties.containsKey("zorka.log.dir")) {
            properties.put("zorka.log.dir", ZorkaUtil.path(homeDir, "log"));
        }

        agentAttrs = mapCfg("zorka.agent", "host", stringCfg("zorka.hostname", System.getenv("HOSTNAME")));
    }

    public Map<String,String> getAgentAttrs() {
        return Collections.unmodifiableMap(agentAttrs);
    }

    /**
     * Adds and configures standard loggers.
     */
    public void initLoggers() {

        FileTrapper.ENABLE_FSYNC = boolCfg("zorka.log.fsync", false);

        if (boolCfg("zorka.filelog", true)) {
            initFileTrapper();
        }

        if (boolCfg("zorka.syslog", false)) {
            initSyslogTrapper();
        }

        // TODO configure logger here
        ZorkaLoggerFactory.getInstance().configure(this.getProperties());

        log.info("Starting ZORKA agent " + get("zorka.version"));
    }


    /**
     * Creates and configures syslogt trapper according to configuration properties
     */
    private void initSyslogTrapper() {
        try {
            String server = stringCfg("zorka.syslog.server", "127.0.0.1");
            String hostname = stringCfg("zorka.hostname", "zorka");
            int syslogFacility = SyslogLib.getFacility(stringCfg("zorka.syslog.facility", "F_LOCAL0"));

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
        String logFileName = stringCfg("zorka.log.fname", "zorka.log");
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4 * 1024 * 1024, maxLogs = 4; // TODO int -> long

        try {
            logThreshold = ZorkaLogLevel.valueOf(stringCfg("zorka.log.level", "INFO"));
            maxSize = (int) (long) kiloCfg("zorka.log.size", 4L * 1024 * 1024);
            maxLogs = intCfg("zorka.log.num", 8);
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

    @Override
    protected void markError(String msg, Throwable e) {
        super.markError(msg, e);
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
    }
}
