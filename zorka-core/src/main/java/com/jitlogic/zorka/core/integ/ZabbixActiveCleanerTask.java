package com.jitlogic.zorka.core.integ;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.jitlogic.zorka.common.model.Data;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

/**
 * Responsible for sending data batches to a "Zabbix Server"
 * @author daiguchi
 *
 */
public class ZabbixActiveCleanerTask implements Runnable {
	/**
	 * Logger
	 */
	private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveCleanerTask.class);

	private ConcurrentLinkedQueue<Data> responseQueue;

	private int maxCacheSize;

	public ZabbixActiveCleanerTask(ConcurrentLinkedQueue<Data> responseQueue, int maxCacheSize){
		this.responseQueue = responseQueue;
		this.maxCacheSize = maxCacheSize;
	}

	@Override
	public void run() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender Cleaning cache...");
		int endIndex = responseQueue.size();
		removeFromCache(endIndex - maxCacheSize);
		log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender ... cache cleaned");
	}

	private void removeFromCache(int itemsCount) {
		/* remove AgentData from cache */
		for (int count = 0; count < itemsCount; count++) {
			responseQueue.poll();
		}
		if (itemsCount > 0) {
			log.debug(ZorkaLogger.ZAG_DEBUG, "ZabbixActiveSender " + itemsCount + " items removed from cache");
		}
	}

}
