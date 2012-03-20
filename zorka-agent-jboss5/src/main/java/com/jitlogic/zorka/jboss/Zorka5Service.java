package com.jitlogic.zorka.jboss;

/* This file is part of ZORKA monitoring agent.
 *
 * ZORKA is free software: you can redistribute it and/or modify it under the
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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.jboss.system.ServiceMBeanSupport;

import com.jitlogic.zorka.agent.TimeoutThreadPoolExecutor;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaUtil;
import com.jitlogic.zorka.agent.zabbix.ZabbixAgent;
import com.jitlogic.zorka.agent.zabbix.ZabbixRequestHandler;

/**
 * 
 * 
 * 
 * @author Rafał Lewczuk <rle@jitlogic.com>
 *
 */
public class Zorka5Service extends ServiceMBeanSupport 
	implements Zorka5ServiceMBean {

	public static final int MAX_THREADS = 5;
	public static final long DEFAULT_TIMEOUT = 3000;
	public static final long DEFAULT_KILL_TIMEOUT = 3000;
	
	private ExecutorService executor;
	private ZorkaBshAgent zorkaAgent = null;
	private ZabbixAgent zabbixAgent = null;
	
	
	protected void startService() {
		log.info("Starting ZORKA agent.");
		
		startZorkaAgent();		
		startZabbixAgent();
		
		log.info("ZORKA agent started.");
	}
	
	
	protected void stopService() {
		stopZabbixAgent();
		stopZorkaAgent();		
	}

		
	public String zoolaQuery(String expr) {
		try {
			return zorkaAgent.query(expr);
		} catch (Throwable e) {
			return "ERROR: " + ZorkaUtil.errorDump(e);
		}
	}
	
	
	public String zabbixQuery(String query) {
		try {
			return zorkaAgent.query(ZabbixRequestHandler.translate(query));
		} catch (Throwable e) {
			return "ERROR: " + ZorkaUtil.errorDump(e);
		}
	}
	
	
	private void startZabbixAgent() {
		Properties props = new Properties();
		String url = System.getProperty("jboss.server.config.url") + "/zorka/zabbix.properties";
		ZorkaUtil.loadProps(url , props);
		if (!props.contains("listen_addr")) 
			props.setProperty("listen_addr", System.getProperty("jboss.bind.address"));
		
		if (props.getProperty("enabled", "no").equalsIgnoreCase("yes")) {
			zabbixAgent = new ZabbixAgent(props, zorkaAgent);
			zabbixAgent.start();
		}
	}

	
	private void startZorkaAgent() {
		
		executor = TimeoutThreadPoolExecutor.newBoundedPool(
				MAX_THREADS, DEFAULT_TIMEOUT, DEFAULT_KILL_TIMEOUT);
	
		zorkaAgent = new ZorkaBshAgent(executor);
		
		zorkaAgent.getZorkaLib().addServer("jboss", getServer());
		
		try {
			String urlpath = System.getProperty("jboss.server.config.url") + "/zorka";
			zorkaAgent.loadScriptDir(new URL(urlpath));
		} catch (MalformedURLException e) {
			log.error("Error loading ZORKA scripts: " + e.getMessage());
		}
		
		zorkaAgent.svcStart();
	}
	
	
	private void stopZabbixAgent() {
		log.info("Stopping ZORKA agent.");
		if (zabbixAgent != null) {
			zabbixAgent.stop();
			zabbixAgent = null;
		}
	}
	
	
	private void stopZorkaAgent() {
		if (zorkaAgent != null) {
			zorkaAgent.svcStop();
			zorkaAgent = null;
		}
		
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
	}
	
	
	public void reload() {
		stopZorkaAgent();
		startZorkaAgent();
		zabbixAgent.setAgent(zorkaAgent);
	}
	
	
	public void clear() {
		zorkaAgent.svcClear();
	}
	

}
