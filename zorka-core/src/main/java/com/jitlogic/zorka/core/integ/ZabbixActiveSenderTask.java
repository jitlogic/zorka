package com.jitlogic.zorka.core.integ;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.model.Data;
import com.jitlogic.zorka.core.util.ZabbixUtils;

public class ZabbixActiveSenderTask implements Runnable {
	/**
	 * Logger
	 */
	private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveSenderTask.class);

	private InetAddress serverAddr;
	private int serverPort;

	private ConcurrentLinkedQueue<Data> responseQueue;

	private long clock;

	private int maxBatchSize;

	private final String _SUCCESS = "success";

	public ZabbixActiveSenderTask(InetAddress serverAddr, int serverPort, ConcurrentLinkedQueue<Data> responseQueue, int maxBatchSize){
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

			ArrayList<Data> listData = new ArrayList<Data>();
			Iterator<Data> iterator = responseQueue.iterator();
			for (int i = 0; i < endIndex; i++) {
				listData.add(iterator.next());
			}

			log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender " + endIndex + " items cached");

			if (endIndex > 0) {
				/* send message */
				String message = ZabbixUtils.createAgentData(listData, clock);

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
