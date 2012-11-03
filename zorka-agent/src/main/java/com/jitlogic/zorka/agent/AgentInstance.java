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

import com.jitlogic.zorka.spy.MainSubmitter;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.zabbix.ZabbixAgent;

import javax.management.MBeanServerConnection;
import java.lang.instrument.ClassFileTransformer;
import java.util.Properties;
import java.util.concurrent.Executor;

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


    private long requestTimeout = DEFAULT_TIMEOUT;
    private int requestThreads = 4;
    private int requestQueue = 64;

    private Executor executor = null;
    private ZorkaBshAgent zorkaAgent = null;
    private ZabbixAgent zabbixAgent = null;

    private SpyLib spyLib = null;
    private SpyInstance spyInstance = null;

    private Properties props;


    public AgentInstance(Properties props) {
        this(props, null);
    }


    public AgentInstance(Properties props, Executor executor) {

        this.props = props;

        try {
            requestTimeout = Long.parseLong(props.getProperty("zorka.req.timeout", "15000").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.timeout setting: '" + props.getProperty("zorka.req.timeout", "15000").trim());
        }

        try {
            requestThreads = Integer.parseInt(props.getProperty("zorka.req.threads", "4").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.threads setting: '" + props.getProperty("zorka.req.threads", "4").trim());
        }

        try {
            requestQueue = Integer.parseInt(props.getProperty("zorka.req.queue", "64").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.queue setting: '" + props.getProperty("zorka.req.queue", "64").trim());
        }
    }


    public  void start() {
        if (executor == null)
            executor = new ClosingTimeoutExecutor(requestThreads, requestQueue, requestTimeout);

        zorkaAgent = new ZorkaBshAgent(executor);

        if (props.getProperty("spy", "no").equalsIgnoreCase("yes")) {
            log.info("Enabling Zorka SPY");
            spyInstance = SpyInstance.instance();
            spyLib = new SpyLib(spyInstance);
            zorkaAgent.installModule("spy", spyLib);
            MainSubmitter.setSubmitter(spyInstance.getSubmitter());
            log.debug("Installed submitter: " + spyInstance.getSubmitter());
        } else {
            log.info("Zorka SPY is diabled. No loaded classes will be transformed in any way.");
        }

        zorkaAgent.loadScriptDir(props.getProperty("zorka.config.dir", ZorkaConfig.DEFAULT_CONFDIR), ".*\\.bsh$");

        zorkaAgent.svcStart();

        if (props.getProperty("zabbix.enabled", "yes").equalsIgnoreCase("yes")) {
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
        }
    }


    public ClassFileTransformer getSpyTransformer() {
        return spyInstance != null ? spyInstance.getClassTransformer() : null;
    }

}
