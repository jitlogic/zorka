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

package com.jitlogic.zorka.core;

import com.jitlogic.zorka.core.mbeans.AttrGetter;
import com.jitlogic.zorka.core.perfmon.PerfMonLib;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogLevel;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import com.jitlogic.zorka.core.integ.*;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.normproc.NormLib;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This method binds together all components to create fuunctional Zorka agent. It is responsible for
 * initializing all subsystems (according to property values in zorka configuration file) and starting
 * all service threads if necessary. It retains references to created components and maintains reference
 * to MBean server registry and its own instance singletons.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentInstance {

    /** Logger */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** MBean server registry */
    private MBeanServerRegistry mBeanServerRegistry;

    /** Executor managing threads that handle requests */
    private Executor executor;

    /** Main zorka agent object - one that executes actual requests */
    private ZorkaBshAgent zorkaAgent;

    /** Reference to zabbix agent object - one that handles zabbix requests and passes them to BSH agent */
    private ZabbixAgent zabbixAgent;

    /** Reference to zabbix library - available to zorka scripts as 'zabbix.*' functions */
    private ZabbixLib zabbixLib;

    /** Reference to nagios agents - one that handles nagios NRPE requests and passes them to BSH agent */
    private NagiosAgent nagiosAgent;

    /** Reference to nagios library - available to zorka scripts as 'nagios.*' functions */
    private NagiosLib nagiosLib;

    /** Reference to spy library - available to zorka scripts as 'spy.*' functions */
    private SpyLib spyLib;

    /** Tracer library */
    private TracerLib tracerLib;

    /** Reference to syslog library - available to zorka scripts as 'syslog.*' functions */
    private SyslogLib syslogLib;

    /** Reference to SNMP library - available to zorka scripts as 'snmp.*' functions */
    private SnmpLib snmpLib;

    /** Reference to normalizers library - available to zorka scripts as 'normalizers.*' function */
    private NormLib normLib;

    /** Reference to ranking and metrics processing library. */
    private PerfMonLib perfMonLib;

    /** Agent configuration properties */
    private Properties props;

    private Tracer tracer;

    private SpyClassTransformer classTransformer;

    private DispatchingSubmitter submitter;

    private ZorkaConfig config;


    public AgentInstance(ZorkaConfig config) {
        this.config = config;
        props = config.getProperties();
    }


    /**
     * Starts agent. Real startup sequence is performed here.
     */
    public  void start() {

        initLoggers(props);


        if (config.boolCfg("spy", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Zorka SPY");
            getZorkaAgent().install("spy", getSpyLib());
            getZorkaAgent().install("tracer", getTracerLib());
        }

        getZorkaAgent().install("perfmon", getPerfMonLib());

        initIntegrationLibs();

        getZorkaAgent().install("normalizers", getNormLib());

        zorkaAgent.loadScriptDir(props.getProperty("zorka.config.dir"), ".*\\.bsh$");

        if (config.boolCfg("zorka.diagnostics", true)) {
            createZorkaDiagMBean();
        }
    }


    public void stop() {
        //instance = null;
    }


    private void initIntegrationLibs() {
        if (config.boolCfg("zabbix", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling ZABBIX subsystem ...");
            getZabbixAgent().start();
            zorkaAgent.install("zabbix", getZabbixLib());
        }

        if (config.boolCfg("syslog", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Syslog subsystem ....");
            zorkaAgent.install("syslog", getSyslogLib());
        }

        if (config.boolCfg("snmp", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling SNMP subsystem ...");
            zorkaAgent.install("snmp", getSnmpLib());
        }

        if (config.boolCfg("nagios", false)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Nagios support.");
            nagiosAgent = new NagiosAgent(config,  zorkaAgent);
            nagiosAgent.start();
            zorkaAgent.install("nagios", getNagiosLib());
        }
    }


    /**
     * Adds and configures standard loggers.
     *
     * @param props configuration properties
     */
    public void initLoggers(Properties props) {
        if (config.boolCfg("zorka.filelog", true)) {
            initFileTrapper();
        }

        if (config.boolCfg("zorka.syslog", false)) {
            initSyslogTrapper();
        }

        ZorkaLogger.configure(props);
    }


    /**
     * Creates and configures syslogt trapper according to configuration properties
     */
    private void initSyslogTrapper() {
        try {
            String server = config.stringCfg("zorka.syslog.server", "127.0.0.1");
            String hostname = config.stringCfg("zorka.hostname", "zorka");
            int syslogFacility = SyslogLib.getFacility(config.stringCfg("zorka.syslog.facility", "F_LOCAL0"));

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
     *
     */
    private void initFileTrapper() {
        String logDir = config.getLogDir();
        boolean logExceptions = config.boolCfg("zorka.log.exceptions", true);
        String logFileName = config.stringCfg("zorka.log.fname", "zorka.log");
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4*1024*1024, maxLogs = 4; // TODO int -> long

        try {
            logThreshold = ZorkaLogLevel.valueOf(config.stringCfg("zorka.log.level", "INFO"));
            maxSize = (int)(long)config.kiloCfg("zorka.log.size", 4L*1024*1024);
            maxLogs = config.intCfg("zorka.log.num", 8);
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


    public void createZorkaDiagMBean() {
        String mbeanName = props.getProperty("zorka.diagnostics.mbean").trim();

        getMBeanServerRegistry().getOrRegister("java", mbeanName, "Version",
            props.getProperty("zorka.version"), "Agent Diagnostics");

        AgentDiagnostics.initMBean(getMBeanServerRegistry(), mbeanName);

        getMBeanServerRegistry().getOrRegister("java", mbeanName, "SymbolsCreated",
            new AttrGetter(getTracer().getSymbolRegistry(), "size()"));
    }


    public ZorkaConfig getConfig() {
        return config;
    }


    private synchronized Executor getExecutor() {
        if (executor == null) {
            int rt = config.intCfg("zorka.req.threads", 8);
            executor = new ThreadPoolExecutor(rt, rt, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(config.intCfg("zorka.req.queue", 64)));
        }
        return executor;
    }



    public synchronized Tracer getTracer() {
        if (tracer == null) {
            tracer = new Tracer();
            MainSubmitter.setTracer(getTracer());
        }
        return tracer;
    }


    public synchronized SpyClassTransformer getClassTransformer() {
        if (classTransformer == null) {
            classTransformer = new SpyClassTransformer(getTracer());
        }
        return classTransformer;
    }

    public synchronized DispatchingSubmitter getSubmitter() {
        if (submitter == null) {
            submitter = new DispatchingSubmitter(getClassTransformer());
        }
        return submitter;
    }

    /**
     * Returns reference to BSH agent.
     * @return instance of Zorka BSH agent
     */
    public synchronized ZorkaBshAgent getZorkaAgent() {
        if (zorkaAgent == null) {
            zorkaAgent = new ZorkaBshAgent(getExecutor(), getMBeanServerRegistry(), config);
        }
        return zorkaAgent;
    }


    public synchronized ZabbixAgent getZabbixAgent() {
        if (zabbixAgent == null) {
            zabbixAgent = new ZabbixAgent(config,  getZorkaAgent());
        }
        return zabbixAgent;
    }


    public synchronized ZabbixLib getZabbixLib() {
        if (zabbixLib == null) {
            zabbixLib = new ZabbixLib(getMBeanServerRegistry(), config);
        }
        return zabbixLib;
    }


    /**
     * Returns reference to syslog library.
     *
     * @return instance of syslog library
     */
    public synchronized SyslogLib getSyslogLib() {
        if (syslogLib == null) {
            syslogLib = new SyslogLib(config);
        }
        return syslogLib;
    }


    /**
     * Returns reference to Spy library
     *
     * @return instance of spy library
     */
    public synchronized SpyLib getSpyLib() {
        if (spyLib == null) {
            spyLib = new SpyLib(getClassTransformer(), getMBeanServerRegistry());
        }
        return spyLib;
    }


    /**
     * Returns reference to tracer library.
     *
     * @return instance of tracer library
     */
    public synchronized TracerLib getTracerLib() {
        if (tracerLib == null) {
            tracerLib = new TracerLib(getTracer(), config);
        }
        return tracerLib;
    }


    public synchronized NagiosLib getNagiosLib() {
        if (nagiosLib == null) {
            nagiosLib = new NagiosLib();
        }
        return nagiosLib;
    }


    public synchronized NormLib getNormLib() {
        if (normLib == null) {
            normLib = new NormLib();
        }
        return normLib;
    }


    /**
     * Returns reference to SNMP library
     *
     * @return instance of snmp library
     */
    public synchronized SnmpLib getSnmpLib() {
        if (snmpLib == null) {
            snmpLib = new SnmpLib(config);
        }
        return snmpLib;
    }


    /**
     * Returns reference to rank processing & metrics library
     *
     * @return instance of perfmon library
     */
    public synchronized PerfMonLib getPerfMonLib() {
        if (perfMonLib == null) {
            perfMonLib = new PerfMonLib(getTracer(), getMBeanServerRegistry());
        }
        return perfMonLib;
    }


    /**
     * Returns reference to mbean server registry.
     *
     * @return mbean server registry reference of null (if not yet initialized)
     */
    public MBeanServerRegistry getMBeanServerRegistry() {
        if (mBeanServerRegistry == null) {
            mBeanServerRegistry = new MBeanServerRegistry(config.boolCfg("zorka.mbs.autoregister", true));
        }
        return mBeanServerRegistry;
    }

}
