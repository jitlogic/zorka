/**
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ.zabbix;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.ZorkaBshAgent;

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

    /** Hostname agent advertises itself to zabbix. */
	private String agentHost;
	private String activeIpPort;

    /** Interval between fetching new item list from Zabbix. */
	private long activeCheckInterval;

    /** Interval between sender cycles */
	private long senderInterval;


	private int maxBatchSize;


	private int maxCacheSize;


	/* Connection Settings */
	private InetAddress activeAddr;
	private String defaultAddr;
	private int activePort;
	private int defaultPort;
	private Socket socket;


	/* Scheduler Management */
	private ScheduledExecutorService scheduler;
	private HashMap<ActiveCheckQueryItem, ScheduledFuture<?>> runningTasks;
	private ConcurrentLinkedQueue<ActiveCheckResult> resultsQueue;
	private ScheduledFuture<?> senderTask;

	/* BSH agent */
	private ZorkaBshAgent agent;

	/* Query translator */
	protected QueryTranslator translator;



	/**
	 * Creates zabbix active agent.
	 */
	public ZabbixActiveAgent(ZorkaConfig config, ZorkaBshAgent agent, QueryTranslator translator, ScheduledExecutorService scheduledExecutorService) {
		this.prefix = "zabbix.active";
		this.config = config;
		this.defaultPort = 10051;
		this.defaultAddr = config.stringCfg("zabbix.server.addr", "127.0.0.1:10051");

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
		activePort = (ipPort.length < 2 || ipPort[1].length() == 0) ? defaultPort : Integer.parseInt(ipPort[1]);

		/* Zabbix Server address */
		try {
			activeAddr = InetAddress.getByName(activeIp.trim());
		} catch (UnknownHostException e) {
			log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse " + prefix + ".server.addr in zorka.properties", e);
			AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
		}

		/* Message */
		ZabbixUtils.setMaxRequestLength(config.intCfg(prefix + ".message.size", 16384));
		
		/* Active Check: Interval, message+hostname */ 
		activeCheckInterval = config.intCfg(prefix + ".check.interval", 120);
		agentHost = config.stringCfg("zorka.hostname", null);

		log.info(ZorkaLogger.ZAG_INFO, "ZabbixActive Agent (" + agentHost + ") will send Active Checks to " + 
				activeAddr + ":" + activePort + " every " + activeCheckInterval + " seconds");

		senderInterval = config.intCfg(prefix + ".sender.interval", 60);
		maxBatchSize = config.intCfg(prefix + ".batch.size", 10);
		maxCacheSize = config.intCfg(prefix + ".cache.size", 150);
		log.info(ZorkaLogger.ZAG_INFO, "ZabbixActive Agent (" + agentHost + ") will send up to " + maxBatchSize + " metrics every " + 
				senderInterval + " seconds. Agent will persist up to " + maxCacheSize + " metrics per " + (senderInterval*2) + 
				" seconds, exceeding records will be discarded.");

		/* scheduler's infra */
		runningTasks = new HashMap<ActiveCheckQueryItem, ScheduledFuture<?>>();
		resultsQueue = new ConcurrentLinkedQueue<ActiveCheckResult>();
	}


	public void start() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive start...");

		if (!running) {
			try {
				socket = new Socket(activeAddr, activePort);
				log.info(ZorkaLogger.ZAG_ERRORS, "Successfuly connected to " + activeIpPort);

			} catch (IOException e) {
				log.error(ZorkaLogger.ZAG_ERRORS, "Failed to connect to " + activeIpPort + ". Will try to connect later.", e);
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
				
				running = true;
				thread = new Thread(this);
				thread.setName("ZORKA-" + prefix + "-main");
				thread.setDaemon(true);
				thread.start();
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
                for (Map.Entry<ActiveCheckQueryItem,ScheduledFuture<?>> e : runningTasks.entrySet()) {
                    e.getValue().cancel(true);
                }
                runningTasks.clear();

				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive clearing dataQueue...");
				resultsQueue.clear();
				
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
		scheduleTasks();

		while (running) {
			try {
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive Refreshing Active Check");

				socket = new Socket(activeAddr, activePort);
				ZabbixActiveRequest request = new ZabbixActiveRequest(socket);

				// send Active Check Message 
				request.sendActiveMessage(agentHost, config.stringCfg(prefix + ".host_metadata", ""));

				// get requests for metrics
				ActiveCheckResponse response = request.getActiveResponse();
//				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive response.toString() " + response.toString());

				if(response.getData() == null) {
				   response.setData(new ArrayList<ActiveCheckQueryItem>());
				}
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
		ArrayList<ActiveCheckQueryItem> newTasks = new ArrayList<ActiveCheckQueryItem>(checkData.getData());
		ArrayList<ActiveCheckQueryItem> tasksToInsert = new ArrayList<ActiveCheckQueryItem>(checkData.getData());
		ArrayList<ActiveCheckQueryItem> tasksToDelete = new ArrayList<ActiveCheckQueryItem>(runningTasks.keySet());

		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - schedule Tasks: " + checkData.toString());

		// Configuring: Insert = New - Running(=tasksToDelete antes da alteração)
		tasksToInsert.removeAll(tasksToDelete);
		
		// Configuring: Delete = Running - New; 
		tasksToDelete.removeAll(newTasks);
		
		// Delete Tasks
		for (ActiveCheckQueryItem task : tasksToDelete) {
			ScheduledFuture<?> taskHandler = runningTasks.get(task);
			taskHandler.cancel(false);
		}

		// Insert Tasks
		for (ActiveCheckQueryItem task : tasksToInsert) {
			ZabbixActiveTask zabbixActiveTask = new ZabbixActiveTask(agentHost, task, agent, translator, resultsQueue);
			ScheduledFuture<?> taskHandler = scheduler.scheduleAtFixedRate(zabbixActiveTask, 5, task.getDelay(), TimeUnit.SECONDS);
			log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - task: " + task.toString());
			runningTasks.put(task, taskHandler);
		}
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - new scheduled tasks: " + tasksToInsert.size());
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActive - deleted old tasks: " + tasksToDelete.size());
		
	}

	private void scheduleTasks() {
		ZabbixActiveSenderTask sender = new ZabbixActiveSenderTask(activeAddr, activePort, resultsQueue, maxBatchSize);
		senderTask = scheduler.scheduleAtFixedRate(sender, senderInterval, senderInterval, TimeUnit.SECONDS);
		
		ZabbixActiveCleanerTask cleaner = new ZabbixActiveCleanerTask(resultsQueue, maxCacheSize);
		senderTask = scheduler.scheduleAtFixedRate(cleaner, senderInterval*2, senderInterval*2, TimeUnit.SECONDS);
	}
}