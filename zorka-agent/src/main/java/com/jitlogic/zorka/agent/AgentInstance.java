/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.integ.NagiosAgent;
import com.jitlogic.zorka.integ.NagiosLib;
import com.jitlogic.zorka.integ.SnmpLib;
import com.jitlogic.zorka.integ.SyslogLib;
import com.jitlogic.zorka.integ.ZabbixLib;
import com.jitlogic.zorka.normproc.NormLib;
import com.jitlogic.zorka.spy.MainSubmitter;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.integ.ZabbixAgent;

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
    private static MBeanServerRegistry mBeanServerRegistry;

    /**
     * Returns reference to mbean server registry.
     *
     * @return mbean server registry reference of null (if not yet initialized)
     */
    public synchronized static MBeanServerRegistry getMBeanServerRegistry() {
        return mBeanServerRegistry;
    }

    /**
     * Sets mbean server registry.
     * @param registry registry
     */
    public synchronized static void setMBeanServerRegistry(MBeanServerRegistry registry) {
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
    public synchronized static AgentInstance instance() {
        if (null == instance) {
            instance = new AgentInstance(ZorkaConfig.getProperties());
            instance.start();
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
    private Executor executor = null;

    /** Main zorka agent object - one that executes actual requests */
    private ZorkaBshAgent zorkaAgent = null;

    /** Reference to zabbix agent object - one that handles zabbix requests and passes them to BSH agent */
    private ZabbixAgent zabbixAgent = null;

    /** Reference to zabbix library - available to zorka scripts as 'zabbix.*' functions */
    private ZabbixLib zabbixLib = null;

    /** Reference to nagios agents - one that handles nagios NRPE requests and passes them to BSH agent */
    private NagiosAgent nagiosAgent = null;

    /** Reference to nagios library - available to zorka scripts as 'nagios.*' functions */
    private NagiosLib nagiosLib = null;

    /** Reference to Spy instrumentation engine object */
    private SpyInstance spyInstance = null;

    /** Reference to spy library - available to zorka scripts as 'spy.*' functions */
    private SpyLib spyLib = null;

    /** Reference to syslog library - available to zorka scripts as 'syslog.*' functions */
    private SyslogLib syslogLib = null;

    /** Reference to SNMP library - available to zorka scripts as 'snmp.*' functions */
    private SnmpLib snmpLib = null;

    /** Reference to normalizers library - available to zorka scripts as 'normalizers.*' function */
    private NormLib normLib = null;

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

        ZorkaLogger.getLogger().init(props);

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
