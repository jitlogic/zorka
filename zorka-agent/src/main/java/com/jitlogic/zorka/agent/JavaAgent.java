/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * 
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.Executor;

import com.jitlogic.zorka.agent.zabbix.ZabbixAgent;
import com.jitlogic.zorka.bootstrap.Agent;
import com.jitlogic.zorka.spy.MainCollector;
import com.jitlogic.zorka.spy.ZorkaSpyLib;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import javax.management.MBeanServerConnection;

public class JavaAgent implements Agent {


	public static final long DEFAULT_TIMEOUT = 60000;

	private final ZorkaLog log = ZorkaLogger.getLog(JavaAgent.class);

    private long requestTimeout = DEFAULT_TIMEOUT;
    private int requestThreads = 4;
    private int requestQueue = 64;

	private Executor executor = null;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;
    private ZorkaSpyLib spyLib = null;

    private MBeanServerRegistry mBeanServerRegistry;

    public JavaAgent() {
        mBeanServerRegistry = new MBeanServerRegistry(
            "yes".equals(ZorkaConfig.get("zorka.mbs.autoregister", "yes")));

        try {
            requestTimeout = Long.parseLong(ZorkaConfig.get("zorka.req.timeout", "15000").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.timeout setting: '" + ZorkaConfig.get("zorka.req.timeout", "15000").trim());
        }

        try {
            requestThreads = Integer.parseInt(ZorkaConfig.get("zorka.req.threads", "4").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.threads setting: '" + ZorkaConfig.get("zorka.req.threads", "4").trim());
        }

        try {
            requestQueue = Integer.parseInt(ZorkaConfig.get("zorka.req.queue", "64").trim());
        } catch (NumberFormatException e) {
            log.error("Invalid zorka.req.queue setting: '" + ZorkaConfig.get("zorka.req.queue", "64").trim());
        }

    }

    public JavaAgent(Executor executor, MBeanServerRegistry mBeanServerRegistry, ZorkaBshAgent bshAgent, ZorkaSpyLib spyLib) {
        this.executor = executor;
        this.mBeanServerRegistry = mBeanServerRegistry;
        this.zorkaAgent = bshAgent;
        this.spyLib = spyLib;
    }

	public  void start() {
        if (executor == null)
            executor = new ClosingTimeoutExecutor(requestThreads, requestQueue, requestTimeout);
        zorkaAgent = new ZorkaBshAgent(executor, mBeanServerRegistry);

        if (ZorkaConfig.get("spy", "no").equalsIgnoreCase("yes")) {
            log.info("Enabling Zorka SPY");
            MainCollector.clear();
            spyLib = new ZorkaSpyLib(zorkaAgent);
            zorkaAgent.installModule("spy", spyLib);
        }

        zorkaAgent.loadScriptDir(ZorkaConfig.getConfDir());

        zorkaAgent.svcStart();

        if (ZorkaConfig.get("zabbix.enabled", "yes").equalsIgnoreCase("yes")) {
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
        }
    }

    public void stop() {
        zabbixAgent.stop();
        zorkaAgent.svcStop();
    }

    public ZorkaBshAgent getZorkaAgent() {
        return zorkaAgent;
    }

    public MBeanServerRegistry getMBeanServerRegistry() {
        return mBeanServerRegistry;
    }

    public ClassFileTransformer getSpyTransformer() {
        return spyLib != null ? spyLib.getSpy() : null;
    }

    public ZorkaSpyLib getSpyLib() {
        return spyLib;
    }

    public void logStart(long id) {
        MainCollector.logStart(id);
    }

    public void logStart(Object[] args, long id) {
        MainCollector.logStart(args, id);
    }

    public void logCall(long id) {
        MainCollector.logCall(id);
    }

    public void logError(long id) {
        MainCollector.logError(id);
    }

    public void registerMbs(String name, MBeanServerConnection conn, ClassLoader classLoader) {
        mBeanServerRegistry.register(name, conn, classLoader);
    }

    public void unregisterMbs(String name) {
        mBeanServerRegistry.unregister(name);
    }

    public void registerBeanAttr(String mbsName, String beanName, String attr, Object val) {
        // TODO use overwriting registration here
        mBeanServerRegistry.getOrRegisterBeanAttr(mbsName, beanName, attr, val, attr);
    }
}
