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

import com.jitlogic.zorka.zabbix.ZabbixAgent;
import com.jitlogic.zorka.spy.SpyInstance;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.spy.MainSubmitter;

public class JavaAgent {

	public static final long DEFAULT_TIMEOUT = 60000;

	private final ZorkaLog log = ZorkaLogger.getLog(JavaAgent.class);

    private long requestTimeout = DEFAULT_TIMEOUT;
    private int requestThreads = 4;
    private int requestQueue = 64;

	private Executor executor = null;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;

    private SpyLib spyLib = null;
    private SpyInstance spyInstance = null;

    private MBeanServerRegistry mBeanServerRegistry;


    public JavaAgent() {
        mBeanServerRegistry = AgentGlobals.getMBeanServerRegistry();

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


	public  void start() {
        if (executor == null)
            executor = new ClosingTimeoutExecutor(requestThreads, requestQueue, requestTimeout);

        zorkaAgent = new ZorkaBshAgent(executor);

        if (ZorkaConfig.get("spy", "no").equalsIgnoreCase("yes")) {
            log.info("Enabling Zorka SPY");
            spyInstance = SpyInstance.instance();
            spyLib = new SpyLib(spyInstance);
            zorkaAgent.installModule("spy", spyLib);
            MainSubmitter.setSubmitter(spyInstance.getSubmitter());
            log.debug("Installed submitter: " + spyInstance.getSubmitter());
        } else {
            log.info("Zorka SPY is diabled. No loaded classes will be transformed in any way.");
        }

        zorkaAgent.loadScriptDir(ZorkaConfig.getConfDir(), ".*\\.bsh$");

        zorkaAgent.svcStart();

        if (ZorkaConfig.get("zabbix.enabled", "yes").equalsIgnoreCase("yes")) {
            zabbixAgent = new ZabbixAgent(zorkaAgent);
            zabbixAgent.start();
        }
    }


    public ClassFileTransformer getSpyTransformer() {
        return spyInstance != null ? spyInstance.getClassTransformer() : null;
    }
}
