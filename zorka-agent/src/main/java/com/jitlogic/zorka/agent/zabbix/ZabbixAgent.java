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

package com.jitlogic.zorka.agent.zabbix;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jitlogic.zorka.agent.ZorkaBshAgent;

public class ZabbixAgent implements Runnable {
	
	private static Logger log = LoggerFactory.getLogger(ZabbixAgent.class);
	
	private volatile ZorkaBshAgent agent;
	
	private String addr;
	private int port = 10055;
	
	private volatile Thread thread;
	private volatile boolean running = false;
	
	private ServerSocket socket;
		
	public ZabbixAgent(Properties props, ZorkaBshAgent agent) {
		this.addr = props.getProperty("listen_addr", "0.0.0.0");
		this.agent = agent;
		try {
			port = Integer.parseInt(props.getProperty("listen_port", "10055"));
		} catch (Exception e) {
			log.error("Invalid 'listen_port' setting in zabbix.properties file. Was '" +
						props.getProperty("listen_port", "10055") + "', should be integer.");
		}
	}
	
	
	public void start() {
		if (!running) {
			try {
				InetAddress iaddr = InetAddress.getByName(addr);
				socket = new ServerSocket(port, 0, iaddr);
				running = true;
				thread = new Thread(this);
				thread.setName("ZORKA-zabbix-main");
				thread.setDaemon(true);
				thread.start();
				log.info("ZORKA-zabbix agent is listening at " + addr + ":" + port + ".");
			} catch (UnknownHostException e) {
				log.error("Error starting ZABBIX agent: unknown address: '" + addr + "'");
			} catch (IOException e) {
				log.error("I/O error while starting zabbix agent:" + e.getMessage());
			}
		}
	}
	
	
	@SuppressWarnings("deprecation")
	public void stop() {
		if (running) {
			running = false;
			try {
				if (socket != null) {
					socket.close();
					socket = null;
				}
				for (int i = 0; i < 100; i++) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) { }
					if (thread == null) return;
				}
				
				log.warn("WARNING: ZORKA-main thread didn't stop " + 
					"after 1000 milliseconds. Shutting down forcibly.");
				thread.stop();
				thread = null;
			} catch (IOException e) {
				log.error("I/O error in zabbix agent main loop: " + e.getMessage());
			}
		} 
	}
	
	
	public void run() {
		while (running) {
			Socket sock = null;
			ZabbixRequestHandler rh = null;
			try {
				sock = socket.accept();
				rh = new ZabbixRequestHandler(sock);
				agent.exec(rh.getReq(), rh);
				sock = null;
			} catch (Exception e) {
				if (running) {
					log.error("Error occured when processing request.", e);
				}
				if (running && rh != null) 
					rh.handleError(e);
			} finally {
				rh = null; sock = null;
			}
			
		}

		thread = null;
		
	}
	
	public void setAgent(ZorkaBshAgent agent) {
		this.agent = agent;
	}
}
