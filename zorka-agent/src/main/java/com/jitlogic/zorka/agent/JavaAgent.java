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

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executor;

import com.jitlogic.zorka.agent.zabbix.ZabbixAgent;
import com.jitlogic.zorka.spy.ZorkaSpyLib;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

public class JavaAgent {


	public static final long DEFAULT_TIMEOUT = 500000;

	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	private Executor executor;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;
    private ZorkaSpyLib spyLib = null;

    private MBeanServerRegistry mBeanServerRegistry = new MBeanServerRegistry();

	public JavaAgent() {
        executor = new ClosingTimeoutExecutor(4, 32, DEFAULT_TIMEOUT);
	}
	
	public void startZorkaAgent() {
		zorkaAgent = new ZorkaBshAgent(executor, mBeanServerRegistry);

        if (ZorkaConfig.get("spy", "no").equalsIgnoreCase("yes")) {
            log.info("Enabling Zorka SPY");
            spyLib = new ZorkaSpyLib(zorkaAgent);
            zorkaAgent.installModule("spy", spyLib);
        }

        zorkaAgent.loadScriptDir(ZorkaConfig.getConfDir());

		zorkaAgent.svcStart();		
	}
	
	public void startZabbixAgent() {		
		if (ZorkaConfig.get("zabbix.enabled", "yes").equalsIgnoreCase("yes")) {
			zabbixAgent = new ZabbixAgent(zorkaAgent);
			zabbixAgent.start();
		}		
	}

	public void stopZabbixAgent() {
		zabbixAgent.stop();
	}
	
	public void stopZorkaAgent() {
		zorkaAgent.svcStop();
	}
	
	private static JavaAgent agent = null;
	
	
	public static void premain(String args, Instrumentation inst) {

		start();

        if (agent.spyLib != null) {
            //log.info("Adding ZORKA class transformer in premain()");
            inst.addTransformer(agent.spyLib.getSpy(), true);
        }
    }

	public static void start() {
		agent = new JavaAgent();
		agent.startZorkaAgent();
		agent.startZabbixAgent();
    }
	
	public static void stop() {
		agent.stopZabbixAgent();
		agent.stopZorkaAgent();
	}

    public static JavaAgent getAgent() {
        return agent;
    }

    public ZorkaBshAgent getZorkaAgent() {
        return zorkaAgent;
    }

    public MBeanServerRegistry getMBeanServerRegistry() {
        return mBeanServerRegistry;
    }
}
