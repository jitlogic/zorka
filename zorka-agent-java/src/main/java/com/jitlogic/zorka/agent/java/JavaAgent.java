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

package com.jitlogic.zorka.agent.java;

import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.jitlogic.zorka.agent.TimeoutThreadPoolExecutor;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.zabbix.ZabbixAgent;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

public final class JavaAgent {

	public static final int MAX_THREADS = 5;
	public static final long DEFAULT_TIMEOUT = 3000;
	public static final long DEFAULT_KILL_TIMEOUT = 3000;

	private static ZorkaLogger log = ZorkaLogger.getLogger(JavaAgent.class);
	
	private ExecutorService executor;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;
	
	
	private JavaAgent() {
		executor = TimeoutThreadPoolExecutor.newBoundedPool(
				MAX_THREADS, DEFAULT_TIMEOUT, DEFAULT_KILL_TIMEOUT);		
	}
	
	
	private void startZorkaAgent() {
		zorkaAgent = new ZorkaBshAgent(executor);
		
		try {
			String urlpath = "file://" + System.getProperty("zorka.config.dir", "/opt/zorka");
			if (!urlpath.contains("://")) {
				urlpath = "file://" + urlpath;
			}
			if (!urlpath.endsWith("/")) {
				urlpath += "/";
			}
			urlpath += "init.d";
			
			zorkaAgent.loadScriptDir(new URL(urlpath));
		} catch (MalformedURLException e) {
			log.error("Error loading ZORKA scripts", e);
		}
		
		zorkaAgent.svcStart();		
	}
	
	
	private void startZabbixAgent() {
		Properties props = new Properties();
		
		String url = System.getProperty("zorka.config.dir", "/opt/zorka");
		if (!url.contains("://")) {
			url = "file://" + url;
		}
		if (!url.endsWith("/")) {
			url += "/";
		}
		url += "zabbix.properties";
		
		ZorkaUtil.loadProps(url , props);
		
		if (props.getProperty("enabled", "yes").equalsIgnoreCase("yes")) {
			zabbixAgent = new ZabbixAgent(props, zorkaAgent);
			zabbixAgent.start();
		}		
	}
	
	
	private void stopZabbixAgent() {
		zabbixAgent.stop();
	}
	
	
	private void stopZorkaAgent() {
		zorkaAgent.svcStop();
	}
	
	
	private static JavaAgent agent = null;
	
	
	public static void premain(String args, Instrumentation inst) {
		start();
	}


	public static void start() {
		log.notice("ZORKA Agent starting...");
		agent = new JavaAgent();
		agent.startZorkaAgent();
		agent.startZabbixAgent();
	}
	
	public static void stop() {
		log.notice("ZORKA agent stopping...");
		agent.stopZabbixAgent();
		agent.stopZorkaAgent();
	}
}
