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

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.util.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

public class ZabbixAgent implements Runnable {
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	private volatile ZorkaBshAgent agent;

    private ZabbixLib zabbixLib;
	
	private String listenAddr;
	private int listenPort = 10055;

    private InetAddress serverAddr = null;

	private volatile Thread thread;
	private volatile boolean running = false;
	
	private ServerSocket socket;
		
	public ZabbixAgent(ZorkaBshAgent agent) {
		this.listenAddr = ZorkaConfig.get("zabbix.listen.addr", "0.0.0.0");
		this.agent = agent;
        this.zabbixLib = new ZabbixLib(agent, agent.getZorkaLib());

        this.agent.installModule("zabbix", zabbixLib);

		try {
			listenPort = Integer.parseInt(ZorkaConfig.get("zabbix.listen.port", "10055"));
		} catch (Exception e) {
			log.error("Invalid 'listen_port' setting in zabbix.properties file. Was '" +
						ZorkaConfig.get("zorka.listen.port", "10055") + "', should be integer.");
		}

        try {
            String sa = ZorkaConfig.get("zabbix.server.addr", "127.0.0.1");
            log.info("Zorka will accept connections from '" + sa.trim() + "'.");
            serverAddr = InetAddress.getByName(sa.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse zabbix.server.addr in zorka.properties", e);
        }
    }
	
	
	public void start() {
		if (!running) {
			try {
				InetAddress iaddr = InetAddress.getByName(listenAddr);
				socket = new ServerSocket(listenPort, 0, iaddr);
				running = true;
				thread = new Thread(this);
				thread.setName("ZORKA-zabbix-main");
				thread.setDaemon(true);
				thread.start();
				log.info("ZORKA-zabbix agent is listening at " + listenAddr + ":" + listenPort + ".");
			} catch (UnknownHostException e) {
				log.error("Error starting ZABBIX agent: unknown address: '" + listenAddr + "'");
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
                if (!sock.getInetAddress().equals(serverAddr)) {
                    log.warn("Illegal connection attempt from '" + sock.getInetAddress() + "'.");
                    sock.close();
                    continue;
                }
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
