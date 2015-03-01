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

import java.util.concurrent.ConcurrentLinkedQueue;

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

	private ConcurrentLinkedQueue<ActiveCheckResult> responseQueue;

	private int maxCacheSize;

	public ZabbixActiveCleanerTask(ConcurrentLinkedQueue<ActiveCheckResult> responseQueue, int maxCacheSize){
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
