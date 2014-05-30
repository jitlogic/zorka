/** 
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.model.ActiveCheckData;
import com.jitlogic.zorka.core.model.ActiveCheckResponse;
import com.jitlogic.zorka.core.model.Data;

/**
 * Zabbix Active Agent integrates Zorka with Zabbix server. It handles incoming zabbix
 * requests and forwards to BSH agent.
 *
 * @author 
 */
public class ZabbixActiveAgent implements Runnable, ZorkaService {

	/* Logger */
	private final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveAgent.class);


	/* Thread */
	private Thread thread;
	private volatile boolean running;


	/* Agent Settings */
	private String prefix;
	private ZorkaConfig config;
	private String agentHost;
	private String activeIpPort;
	private long activeCheckInterval;
	private long senderInterval;
	private int maxBatchSize;


	/* Connection Settings */
	private InetAddress activeAddr;
	private String defaultAddr;
	private int activePort;
	private int defaultPort;
	private Socket socket;


	/* Scheduler Management */
	private ScheduledExecutorService scheduler;
	private HashMap<ActiveCheckData, ScheduledFuture<?>> runningTasks;
	private ConcurrentLinkedQueue<Data> dataQueue;
	private ScheduledFuture<?> senderTask;

	/* BSH agent */
	private ZorkaBshAgent agent;

	/* Query translator */
	protected QueryTranslator translator;



	/**
	 * Creates zabbix active agent.
	 */
	public ZabbixActiveAgent(ZorkaConfig config, ZorkaBshAgent agent, QueryTranslator translator, ScheduledExecutorService scheduledExecutorService) {
		this.prefix = "zabbixActive";
		this.config = config;
		this.defaultPort = 10055;
		this.defaultAddr = "127.0.0.1:10055";

		this.scheduler = scheduledExecutorService;

		this.agent = agent;
		this.translator = translator;

		setup();
	}


	protected void setup() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive setup...");

		/* Zabbix Server IP:Port */
		activeIpPort = config.stringCfg(prefix + ".server.addr", defaultAddr);
		String [] ipPort = activeIpPort.split(":");  
		String activeIp = ipPort[0];
		activePort = (ipPort[1] == null || ipPort[1].isEmpty())? defaultPort : Integer.parseInt(ipPort[1]);

		/* Zabbix Server address */
		try {
			activeAddr = InetAddress.getByName(activeIp.trim());
		} catch (UnknownHostException e) {
			log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse " + prefix + ".server.addr in zorka.properties", e);
			AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
		}

		/* Active Check: Interval, message+hostname */ 
		activeCheckInterval = config.intCfg(prefix + ".check.interval", 120);
		agentHost = config.stringCfg("zorka.hostname", null);

		log.info(ZorkaLogger.ZAG_INFO, "ZabbixActive Agent (" + agentHost + ") will send Active Checks to " + 
				activeIpPort + " every " + activeCheckInterval + " seconds");

		senderInterval = config.intCfg(prefix + ".sender.interval", 60);
		maxBatchSize = config.intCfg(prefix + ".batch.size", 10);
		log.info(ZorkaLogger.ZAG_INFO, "ZabbixActive Agent (" + agentHost + ") will send up to " + maxBatchSize + " metrics every " + 
				senderInterval + " seconds");

		/* scheduler's infra */
		runningTasks = new HashMap<ActiveCheckData, ScheduledFuture<?>>();
		dataQueue = new ConcurrentLinkedQueue<Data>();
	}


	public void start() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive start...");

		if (!running) {
			try {
				socket = new Socket(activeAddr, activePort);
				log.info(ZorkaLogger.ZAG_ERRORS, "Successfuly connected to " + activeIpPort);
				running = true;

				thread = new Thread(this);
				thread.setName("ZORKA-" + prefix + "-main");
				thread.setDaemon(true);
				thread.start();
			} catch (IOException e) {
				log.error(ZorkaLogger.ZAG_ERRORS, "Failed to connect to " + activeIpPort, e);
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						socket = null;
					}
				}
			}
		}
	}


	@SuppressWarnings("deprecation")
	public void stop() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive stop...");
		if (running) {
			running = false;
			try {
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive cancelling sender task...");
				senderTask.cancel(true);
				
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive cancelling all ZorkaBsh tasks...");
				for (ActiveCheckData task : runningTasks.keySet()) {
					ScheduledFuture<?> future = runningTasks.remove(task);
					future.cancel(true);					
				}
				
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive clearing dataQueue...");
				dataQueue.clear();
				
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive closing socket...");
				if (socket != null) {
					socket.close();
					socket = null;
				}
				for (int i = 0; i < 100; i++) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (thread == null) {
						return;
					}
				}

				log.warn(ZorkaLogger.ZAG_WARNINGS, "ZORKA-" + prefix + " thread didn't stop after 1000 milliseconds. Shutting down forcibly.");

				thread.stop();
				thread = null;
			} catch (IOException e) {
				log.error(ZorkaLogger.ZAG_ERRORS, "I/O error in zabbix core main loop: " + e.getMessage());
			}
		}
	}


	public void restart() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive restart...");
		setup();
		start();
	}


	@Override
	public void shutdown() {
		log.info(ZorkaLogger.ZAG_CONFIG, "Shutting down " + prefix + " agent ...");
		stop();
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
	}


	@Override
	public void run() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive run...");

		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive Scheduling sender task");
		scheduleSender();

		while (running) {
			try {
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive Refreshing Active Check");

				socket = new Socket(activeAddr, activePort);
				ZabbixActiveRequest request = new ZabbixActiveRequest(socket);

				// send Active Check Message 
				request.sendActiveMessage(agentHost);

				// get requests for metrics
				ActiveCheckResponse response = request.getActiveResponse();
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive response.toString() " + response.toString());

				// Schedule all requests
				scheduleTasks(response);
			} catch (IOException e) {
				log.error(ZorkaLogger.ZAG_ERRORS, "Failed to connect to " + activeIpPort, e);
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						socket = null;
					}
				}
			}
			
			try {
				Thread.sleep(activeCheckInterval * 1000L);
			} catch (InterruptedException e) {
				log.error(ZorkaLogger.ZAG_ERRORS, "Failed to sleep", e);
			}
		}
	}

	private void scheduleTasks(ActiveCheckResponse checkData) {
		ArrayList<ActiveCheckData> newTasks = new ArrayList<ActiveCheckData>(checkData.getData());
		ArrayList<ActiveCheckData> tasksToInsert = new ArrayList<ActiveCheckData>(checkData.getData());
		ArrayList<ActiveCheckData> tasksToDelete = new ArrayList<ActiveCheckData>(runningTasks.keySet());

		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - schedule Tasks: " + checkData.toString());

		// Configuring: Insert = New - Running(=tasksToDelete antes da alteração)
		tasksToInsert.removeAll(tasksToDelete);
		
		// Configuring: Delete = Running - New; 
		tasksToDelete.removeAll(newTasks);
		
		// Delete Tasks
		for (ActiveCheckData task : tasksToDelete) {
			ScheduledFuture<?> taskHandler = runningTasks.get(task);
			taskHandler.cancel(false);
		}
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - deleted tasks: " + tasksToDelete.size());

		// Insert Tasks
		for (ActiveCheckData task : tasksToInsert) {
			ZabbixActiveTask zabbixActiveTask = new ZabbixActiveTask(agentHost, task, agent, translator,dataQueue);			
			ScheduledFuture<?> taskHandler = scheduler.scheduleAtFixedRate(zabbixActiveTask, 5, task.getDelay(), TimeUnit.SECONDS);
			runningTasks.put(task, taskHandler);
		}
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - scheduled tasks: " + tasksToInsert.size());
	}

	private void scheduleSender() {
		ZabbixActiveSenderTask sender = new ZabbixActiveSenderTask(activeAddr, activePort, dataQueue, maxBatchSize);			
		senderTask = scheduler.scheduleAtFixedRate(sender, senderInterval, senderInterval, TimeUnit.SECONDS);
	}
}