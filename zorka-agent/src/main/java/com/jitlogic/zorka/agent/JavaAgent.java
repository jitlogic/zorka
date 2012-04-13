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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import com.jitlogic.zorka.agent.zabbix.ZabbixAgent;
import com.jitlogic.zorka.util.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLogger;

public class JavaAgent {

	public static final int MAX_THREADS = 5;
	public static final long DEFAULT_TIMEOUT = 3000;
	public static final long DEFAULT_KILL_TIMEOUT = 3000;

	private static ZorkaLogger log = ZorkaLogger.getLogger(JavaAgent.class);
	
	private ExecutorService executor;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;
		
	public JavaAgent() {
		executor = TimeoutThreadPoolExecutor.newBoundedPool(
				MAX_THREADS, DEFAULT_TIMEOUT, DEFAULT_KILL_TIMEOUT);
	}
	
	public void startZorkaAgent() {
		zorkaAgent = new ZorkaBshAgent(executor);
		
		try {
			zorkaAgent.loadScriptDir(new URL("file://" + ZorkaConfig.getConfDir()));
		} catch (MalformedURLException e) {
			log.error("Error loading ZORKA scripts", e);
		}
		
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
	
}
