/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.integ.SyslogLib;
import com.jitlogic.zorka.core.integ.SyslogTrapper;

import java.io.File;
import java.util.*;

public class AgentConfig extends ZorkaConfig {

    private static final ZorkaLog log = ZorkaLogger.getLog(AgentConfig.class);

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/core/zorka.properties";
    public static final String PROP_SCRIPTS_DIR = "zorka.scripts.dir";
    ;
    public static final String PROP_PROFILE_DIR = "zorka.profile.dir";

    private String agentHome;

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
        homeDir = props.getProperty(PROP_HOME_DIR);
        setBaseProps();
    }


    protected void setBaseProps() {
        if (!properties.containsKey(PROP_SCRIPTS_DIR)) {
            properties.put(PROP_SCRIPTS_DIR, ZorkaUtil.path(homeDir, "scripts"));
        }

        if (!properties.containsKey(PROP_PROFILE_DIR)) {
            properties.put(PROP_PROFILE_DIR, ZorkaUtil.path(homeDir, "profiles"));
        }

        if (!properties.containsKey("zorka.log.dir")) {
            properties.put("zorka.log.dir", ZorkaUtil.path(homeDir, "log"));
        }
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

        ZorkaLogger.configure(getProperties());

        log.info(ZorkaLogger.ZAG_CONFIG, "Starting ZORKA agent " + getProperties().getProperty("zorka.version"));
    }


    /**
     * Creates and configures syslogt trapper according to configuration properties
     */
    private void initSyslogTrapper() {
        try {
            String server = stringCfg("zorka.syslog.server", "127.0.0.1");
            String hostname = stringCfg("zorka.hostname", "zorka");
            int syslogFacility = SyslogLib.getFacility(stringCfg("zorka.syslog.facility", "F_LOCAL0"));

            SyslogTrapper syslog = new SyslogTrapper(server, hostname, syslogFacility, true);
            syslog.disableTrapCounter();
            syslog.start();

            ZorkaLogger.getLogger().addTrapper(syslog);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error parsing logger arguments", e);
            log.info(ZorkaLogger.ZAG_ERRORS, "Syslog trapper will be disabled.");
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Error parsing logger arguments", e);
            log.info(ZorkaLogger.ZAG_ERRORS, "File trapper will be disabled.");
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }


        FileTrapper trapper = FileTrapper.rolling(logThreshold,
                new File(logDir, logFileName).getPath(), maxLogs, maxSize, logExceptions);
        trapper.disableTrapCounter();
        trapper.start();

        ZorkaLogger.getLogger().addTrapper(trapper);
    }


    @Override
    protected void markError(String msg, Throwable e) {
        super.markError(msg, e);
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
    }
}
