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

import com.jitlogic.zorka.integ.nagios.NagiosAgent;
import com.jitlogic.zorka.integ.nagios.NagiosLib;
import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.syslog.SyslogLib;
import com.jitlogic.zorka.logproc.LogProcLib;
import com.jitlogic.zorka.normproc.NormLib;
import com.jitlogic.zorka.spy.MainSubmitter;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.integ.zabbix.ZabbixAgent;

import javax.management.MBeanServerConnection;
import java.lang.instrument.ClassFileTransformer;
import java.util.Properties;
import java.util.concurrent.Executor;

import static com.jitlogic.zorka.agent.ZorkaConfig.*;

public class AgentInstance {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    public static final long DEFAULT_TIMEOUT = 60000;
    private static MBeanServerRegistry mBeanServerRegistry;

    public synchronized static MBeanServerConnection lookupMBeanServer(String name) {
        return mBeanServerRegistry != null ? mBeanServerRegistry.lookup(name) : null;
    }

    public synchronized static MBeanServerRegistry getMBeanServerRegistry() {
        return mBeanServerRegistry;
    }

    public synchronized static void setMBeanServerRegistry(MBeanServerRegistry registry) {
        mBeanServerRegistry = registry;
    }

    private static AgentInstance instance = null;

    public static AgentInstance instance() {
        if (null == instance) {
            instance = new AgentInstance(ZorkaConfig.getProperties());
            instance.start();
        }

        return instance;
    }


    public static void setInstance(AgentInstance newInstance) {
        instance = newInstance;
    }


    private long requestTimeout = DEFAULT_TIMEOUT;
    private int requestThreads = 4;
    private int requestQueue = 64;

    private Executor executor = null;
    private ZorkaBshAgent zorkaAgent = null;
    private ZabbixAgent zabbixAgent = null;

    private NagiosAgent nagiosAgent = null;
    private NagiosLib nagiosLib = null;

    private SpyLib spyLib = null;
    private SpyInstance spyInstance = null;

    private SyslogLib syslogLib = null;
    private SnmpLib snmpLib = null;

    private NormLib normLib = null;
    private LogProcLib logProcLib = null;

    private Properties props;


    public AgentInstance(Properties props) {
        this(props, null);
    }


    public AgentInstance(Properties props, Executor executor) {

        this.props = props;

        try {
            requestTimeout = Long.parseLong(props.getProperty(ZORKA_REQ_TIMEOUT).trim());
        } catch (NumberFormatException e) {
            log.error("Invalid " + ZORKA_REQ_TIMEOUT +  " property: '" + props.getProperty(ZORKA_REQ_TIMEOUT).trim() + "'");
        }

        try {
            requestThreads = Integer.parseInt(props.getProperty(ZORKA_REQ_THREADS).trim());
        } catch (NumberFormatException e) {
            log.error("Invalid " + ZORKA_REQ_THREADS + " setting: '" + props.getProperty(ZORKA_REQ_THREADS).trim() + "'");
        }

        try {
            requestQueue = Integer.parseInt(props.getProperty(ZORKA_REQ_QUEUE).trim());
        } catch (NumberFormatException e) {
            log.error("Invalid " + ZORKA_REQ_QUEUE + "setting: '" + props.getProperty(ZORKA_REQ_QUEUE).trim() + "'");
        }
    }


    public  void start() {
        if (executor == null)
            executor = new ClosingTimeoutExecutor(requestThreads, requestQueue, requestTimeout);

        zorkaAgent = new ZorkaBshAgent(executor);

        normLib = new NormLib();
        zorkaAgent.installModule("normalizers", normLib);

        logProcLib = new LogProcLib();
        zorkaAgent.installModule("logproc", logProcLib);

        if ("yes".equalsIgnoreCase(props.getProperty(SPY_ENABLE))) {
            log.info("Enabling Zorka SPY");
            spyInstance = SpyInstance.instance();
            spyLib = new SpyLib(spyInstance);
            zorkaAgent.installModule("spy", spyLib);
            MainSubmitter.setSubmitter(spyInstance.getSubmitter());
            log.debug("Installed submitter: " + spyInstance.getSubmitter());
        } else {
            log.info("Zorka SPY is diabled. No loaded classes will be transformed in any way.");
        }

        if ("yes".equalsIgnoreCase(props.getProperty(SYSLOG_ENABLE))) {
            log.info("Enabling Syslog subsystem ....");
            syslogLib = new SyslogLib();
            zorkaAgent.installModule("syslog", syslogLib);
        }

        if ("yes".equalsIgnoreCase(props.getProperty(SNMP_ENABLE))) {
            log.info("Enabling SNMP subsystem ...");
            snmpLib = new SnmpLib();
            zorkaAgent.installModule("snmp", snmpLib);
        }

        if ("yes".equalsIgnoreCase(props.getProperty(ZABBIX_ENABLE))) {
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
        }

        if ("yes".equalsIgnoreCase(props.getProperty(NAGIOS_ENABLE))) {
            nagiosAgent = new NagiosAgent(zorkaAgent);
            nagiosLib = new NagiosLib();
            zorkaAgent.installModule("nagios", nagiosLib);
            nagiosAgent.start();
        }


        zorkaAgent.loadScriptDir(props.getProperty(ZORKA_CONF_DIR), ".*\\.bsh$");

    }


    public ClassFileTransformer getSpyTransformer() {
        return spyInstance != null ? spyInstance.getClassTransformer() : null;
    }


    public SpyInstance getSpyInstance() {
        return spyInstance;
    }

    public ZorkaBshAgent getZorkaAgent() {
        return zorkaAgent;
    }

    public SyslogLib getSyslogLib() {
        return syslogLib;
    }

    public SpyLib getSpyLib() {
        return spyLib;
    }

    public SnmpLib getSnmpLib() {
        return snmpLib;
    }

    public NormLib getNormLib() {
        return normLib;
    }

    public LogProcLib getLogProcLib() {
        return logProcLib;
    }

}
