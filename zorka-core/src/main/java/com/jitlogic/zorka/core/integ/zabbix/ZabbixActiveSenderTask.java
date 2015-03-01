/**
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.integ.zabbix;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

public class ZabbixActiveSenderTask implements Runnable {
	/**
	 * Logger
	 */
	private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveSenderTask.class);

	private InetAddress serverAddr;
	private int serverPort;

	private ConcurrentLinkedQueue<ActiveCheckResult> responseQueue;

	private long clock;

	private int maxBatchSize;

	private final String _SUCCESS = "success";

	public ZabbixActiveSenderTask(InetAddress serverAddr, int serverPort, ConcurrentLinkedQueue<ActiveCheckResult> responseQueue, int maxBatchSize){
		this.serverAddr = serverAddr;
		this.serverPort = serverPort;
		this.responseQueue = responseQueue;
		this.maxBatchSize = maxBatchSize;
	}

	@Override
	public void run() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender run...");
		Socket socket = null;
		try {
			clock = (new Date()).getTime() / 1000L;

			socket = new Socket(serverAddr, serverPort);
			ZabbixActiveRequest request = new ZabbixActiveRequest(socket);

			/* copy cache */
			int endIndex = responseQueue.size();
			endIndex = (endIndex > maxBatchSize)? maxBatchSize : endIndex; 

			ArrayList<ActiveCheckResult> results = new ArrayList<ActiveCheckResult>();
			Iterator<ActiveCheckResult> iterator = responseQueue.iterator();
			for (int i = 0; i < endIndex; i++) {
				results.add(iterator.next());
			}

			log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender " + endIndex + " items cached");

			if (results.size() > 0) {
				/* send message */
				String message = ZabbixUtils.createAgentData(results, clock);

				request.send(message);
				log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender message sent: " + message);

				/* verify OK */
				String response = request.getReq();
				if (response.contains(_SUCCESS)) {
					/* remove AgentData from cache */
					for (int count = 0; count < endIndex; count++) {
						responseQueue.poll();
					}
					log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender " + endIndex + " items removed from cache");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender finished");
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
