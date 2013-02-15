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

package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.agent.mbeans.AttrGetter;
import com.jitlogic.zorka.agent.perfmon.PerfMonLib;
import com.jitlogic.zorka.agent.spy.TracerLib;
import com.jitlogic.zorka.common.*;
import com.jitlogic.zorka.agent.integ.*;
import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.agent.normproc.NormLib;
import com.jitlogic.zorka.agent.spy.MainSubmitter;
import com.jitlogic.zorka.agent.spy.SpyInstance;
import com.jitlogic.zorka.agent.spy.SpyLib;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
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
    private static volatile MBeanServerRegistry mBeanServerRegistry;

    /**
     * Returns reference to mbean server registry.
     *
     * @return mbean server registry reference of null (if not yet initialized)
     */
    public static MBeanServerRegistry getMBeanServerRegistry() {
        return mBeanServerRegistry;
    }

    /**
     * Sets mbean server registry.
     * @param registry registry
     */
    public static void setMBeanServerRegistry(MBeanServerRegistry registry) {
        mBeanServerRegistry = registry;
    }

    /**
     * Agent instance (singleton)
     */
    private static AgentInstance instance = null;

    /**
     * Returns agent instance (creates one if not done it yet).
     *
     * @return agent instance
     */
    public static AgentInstance instance() {

        synchronized (AgentInstance.class) {
            if (null == instance) {
                instance = new AgentInstance(ZorkaConfig.getProperties());
                instance.start();
            }
        }

        return instance;
    }


    /**
     * TODO this is used only in test scenarios, get rid of it.
     *
     * Sets (preconfigured) agent instance.
     *
     * @param newInstance agent instance to be set
     */
    public static void setInstance(AgentInstance newInstance) {
        instance = newInstance;
    }

    /** Number of threads handling requests */
    private int requestThreads;

    /** Size of request queue */
    private int requestQueue;

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

    /** Reference to Spy instrumentation engine object */
    private SpyInstance spyInstance;

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


    /**
     * Standard constructor. It only reads some configuration properties, no real startup is actually done.
     *
     * @param props configuration properties
     */
    public AgentInstance(Properties props) {
        this.props = props;

        requestThreads = ZorkaConfig.intCfg("zorka.req.threads", 4);
        requestQueue = ZorkaConfig.intCfg("zorka.req.queue", 64);
    }


    /**
     * Starts agent. Real startup sequence is performed here.
     */
    public  void start() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(requestThreads, requestThreads, 1000, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(requestQueue));
        }

        initLoggers(props);

        zorkaAgent = new ZorkaBshAgent(executor);

        normLib = new NormLib();
        zorkaAgent.install("normalizers", normLib);

        if (ZorkaConfig.boolCfg("spy", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Zorka SPY");
            spyInstance = SpyInstance.instance();
            spyLib = new SpyLib(spyInstance);
            zorkaAgent.install("spy", spyLib);
            tracerLib = new TracerLib(spyInstance);
            zorkaAgent.install("tracer", tracerLib);
            MainSubmitter.setSubmitter(spyInstance.getSubmitter());
        } else {
            log.info(ZorkaLogger.ZAG_CONFIG, "Zorka SPY is diabled. No loaded classes will be transformed in any way.");
        }

        perfMonLib = new PerfMonLib(spyInstance);
        zorkaAgent.install("perfmon", perfMonLib);

        initIntegrationLibs();

        zorkaAgent.loadScriptDir(props.getProperty("zorka.config.dir"), ".*\\.bsh$");

        if ("yes".equalsIgnoreCase(props.getProperty("zorka.diagnostics"))) {
            createZorkaDiagMBean();
        }
    }


    private void initIntegrationLibs() {
        if (ZorkaConfig.boolCfg("zabbix", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling ZABBIX subsystem ...");
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
            zabbixLib = new ZabbixLib();
            zorkaAgent.install("zabbix", zabbixLib);
        }

        if (ZorkaConfig.boolCfg("syslog", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Syslog subsystem ....");
            syslogLib = new SyslogLib();
            zorkaAgent.install("syslog", syslogLib);
        }

        if (ZorkaConfig.boolCfg("snmp", true)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling SNMP subsystem ...");
            snmpLib = new SnmpLib();
            zorkaAgent.install("snmp", snmpLib);
        }

        if (ZorkaConfig.boolCfg("nagios", false)) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Enabling Nagios support.");
            nagiosAgent = new NagiosAgent(zorkaAgent);
            nagiosLib = new NagiosLib();
            zorkaAgent.install("nagios", nagiosLib);
            nagiosAgent.start();
        }
    }


    /**
     * Adds and configures standard loggers.
     *
     * @param props configuration properties
     */
    public void initLoggers(Properties props) {
        initFileTrapper();

        if (ZorkaConfig.boolCfg("zorka.syslog", false)) {
            initSyslogTrapper();
        }

        ZorkaLogger.configure(props);
    }


    /**
     * Creates and configures syslogt trapper according to configuration properties
     */
    private void initSyslogTrapper() {
        try {
            String server = ZorkaConfig.stringCfg("zorka.syslog.server", "127.0.0.1");
            String hostname = ZorkaConfig.stringCfg("zorka.hostname", "zorka");
            int syslogFacility = SyslogLib.getFacility(ZorkaConfig.stringCfg("zorka.syslog.facility", "F_LOCAL0"));

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
        String logDir = ZorkaConfig.getLogDir();
        boolean logExceptions = ZorkaConfig.boolCfg("zorka.log.exceptions", true);
        String logFileName = ZorkaConfig.stringCfg("zorka.log.fname", "zorka.log");
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4*1024*1024, maxLogs = 4;

        try {
            logThreshold = ZorkaLogLevel.valueOf(ZorkaConfig.stringCfg("zorka.log.level", "INFO"));
            maxSize = (int)(long)ZorkaConfig.kiloCfg("zorka.log.size", 4L*1024*1024);
            maxLogs = ZorkaConfig.intCfg("zorka.log.num", 8);
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

        mBeanServerRegistry.getOrRegister("java", mbeanName, "Version",
            props.getProperty("zorka.version"), "Agent Diagnostics");

        AgentDiagnostics.initMBean(mBeanServerRegistry, mbeanName);

        mBeanServerRegistry.getOrRegister("java", mbeanName, "SymbolsCreated",
            new AttrGetter(spyInstance.getTracer().getSymbolRegistry(), "size()"));
    }


    /**
     * Return class file transformer of Spy instrumentation engine or null if spy is disabled.
     *
     * @return class file transformer
     */
    public ClassFileTransformer getSpyTransformer() {
        return spyInstance != null ? spyInstance.getClassTransformer() : null;
    }


    /**
     * Returns reference to Spy instrumentation engine.
     *
     * @return instance of spy instrumentation engine
     */
    public SpyInstance getSpyInstance() {
        return spyInstance;
    }

    /**
     * Returns reference to BSH agent.
     * @return instance of Zorka BSH agent
     */
    public ZorkaBshAgent getZorkaAgent() {
        return zorkaAgent;
    }

    /**
     * Returns reference to syslog library.
     *
     * @return instance of syslog library
     */
    public SyslogLib getSyslogLib() {
        return syslogLib;
    }

    /**
     * Returns reference to Spy library
     *
     * @return instance of spy library
     */
    public SpyLib getSpyLib() {
        return spyLib;
    }

    /**
     * Returns reference to tracer library.
     *
     * @return instance of tracer library
     */
    public TracerLib getTracerLib() {
        return tracerLib;
    }

    /**
     * Returns reference to SNMP library
     *
     * @return instance of snmp library
     */
    public SnmpLib getSnmpLib() {
        return snmpLib;
    }

    /**
     * Returns reference to rank processing & metrics library
     *
     * @return instance of perfmon library
     */
    public PerfMonLib getPerfMonLib() {
        return perfMonLib;
    }

}
