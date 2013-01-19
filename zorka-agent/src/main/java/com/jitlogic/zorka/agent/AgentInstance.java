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

import com.jitlogic.zorka.integ.*;
import com.jitlogic.zorka.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.normproc.NormLib;
import com.jitlogic.zorka.spy.MainSubmitter;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogConfig;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

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
    private int requestThreads = 4;

    /** Size of request queue */
    private int requestQueue = 64;

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

    /** Reference to syslog library - available to zorka scripts as 'syslog.*' functions */
    private SyslogLib syslogLib;

    /** Reference to SNMP library - available to zorka scripts as 'snmp.*' functions */
    private SnmpLib snmpLib;

    /** Reference to normalizers library - available to zorka scripts as 'normalizers.*' function */
    private NormLib normLib;

    /** Agent configuration properties */
    private Properties props;

    /**
     * Standard constructor. It only reads some configuration properties, no real startup is actually done.
     *
     * @param props configuration properties
     */
    public AgentInstance(Properties props) {

        this.props = props;

        try {
            requestThreads = Integer.parseInt(props.getProperty("zorka.req.threads").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid " + "zorka.req.threads" + " setting: '" + props.getProperty("zorka.req.threads").trim() + "'");
        }

        try {
            requestQueue = Integer.parseInt(props.getProperty("zorka.req.queue").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid " + "zorka.req.queue" + "setting: '" + props.getProperty("zorka.req.queue").trim() + "'");
        }
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

        if ("yes".equalsIgnoreCase(props.getProperty("spy"))) {
            log.info("Enabling Zorka SPY");
            spyInstance = SpyInstance.instance();
            spyLib = new SpyLib(spyInstance);
            zorkaAgent.install("spy", spyLib);
            MainSubmitter.setSubmitter(spyInstance.getSubmitter());
            log.debug("Installed submitter: " + spyInstance.getSubmitter());
        } else {
            log.info("Zorka SPY is diabled. No loaded classes will be transformed in any way.");
        }

        if ("yes".equalsIgnoreCase(props.getProperty("zabbix"))) {
            log.info("Enabling ZABBIX subsystem ...");
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
            zabbixLib = new ZabbixLib(zorkaAgent,  zorkaAgent.getZorkaLib());
            zorkaAgent.install("zabbix", zabbixLib);
        }

        if ("yes".equalsIgnoreCase(props.getProperty("syslog"))) {
            log.info("Enabling Syslog subsystem ....");
            syslogLib = new SyslogLib();
            zorkaAgent.install("syslog", syslogLib);
        }

        if ("yes".equalsIgnoreCase(props.getProperty("snmp"))) {
            log.info("Enabling SNMP subsystem ...");
            snmpLib = new SnmpLib();
            zorkaAgent.install("snmp", snmpLib);
        }

        if ("yes".equalsIgnoreCase(props.getProperty("nagios"))) {
            nagiosAgent = new NagiosAgent(zorkaAgent);
            nagiosLib = new NagiosLib();
            zorkaAgent.install("nagios", nagiosLib);
            nagiosAgent.start();
        }


        zorkaAgent.loadScriptDir(props.getProperty("zorka.config.dir"), ".*\\.bsh$");

    }


    /**
     * Adds and configures standard loggers.
     *
     * @param props configuration properties
     */
    public void initLoggers(Properties props) {
        initFileTrapper(props);

        if ("yes".equalsIgnoreCase(props.getProperty("zorka.syslog", "no").trim())) {
            initSyslogTrapper(props);
        }

        ZorkaLogConfig.configure(props);
    }

    /**
     * Creates and configures syslog trapper according to configuration properties
     *
     * @param props configuration properties
     */
    private void initSyslogTrapper(Properties props) {
        try {
            String server = props.getProperty("zorka.syslog.server", "127.0.0.1").trim();
            String hostname = props.getProperty("zorka.hostname", "zorka").trim();
            int syslogFacility = SyslogLib.getFacility(props.getProperty("zorka.syslog.facility", "F_LOCAL0").trim());

            SyslogTrapper syslog = new SyslogTrapper(server, hostname, syslogFacility, true);
            syslog.start();

            ZorkaLogger.getLogger().addTrapper(syslog);
        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Syslog trapper will be disabled.");
        }
    }


    /**
     * Creates and configures file trapper according to configuration properties
     *
     * @param props configuration properties
     */
    private void initFileTrapper(Properties props) {
        String logDir = ZorkaConfig.getLogDir();
        boolean logExceptions = "yes".equalsIgnoreCase(props.getProperty("zorka.log.exceptions"));
        String logFileName = props.getProperty("zorka.log.fname").trim();
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4*1024*1024, maxLogs = 4;

        try {
            logThreshold = ZorkaLogLevel.valueOf (props.getProperty("zorka.log.level"));
            maxSize = (int) ZorkaUtil.parseIntSize(props.getProperty("zorka.log.size").trim());
            maxLogs = (int)ZorkaUtil.parseIntSize(props.getProperty("zorka.log.num").trim());
        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
        }


        FileTrapper trapper = FileTrapper.rolling(logThreshold,
                new File(logDir, logFileName).getPath(), maxLogs, maxSize, logExceptions);

        trapper.start();

        ZorkaLogger.getLogger().addTrapper(trapper);
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
     * Returns reference to SNMP library
     *
     * @return instance of snmp library
     */
    public SnmpLib getSnmpLib() {
        return snmpLib;
    }

}
