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
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.integ.ZorkaRequestHandler;

public class ZabbixActiveTask implements Runnable, ZorkaRequestHandler {

	/* Logger */
	private final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveTask.class);
	
	private String agentHost;
	private ActiveCheckQueryItem item;
	private ZorkaBshAgent agent;
	private QueryTranslator translator;
	private ConcurrentLinkedQueue<ActiveCheckResult> responseQueue;
	
	private long clock;
	
	public ZabbixActiveTask(String agentHost, ActiveCheckQueryItem item, ZorkaBshAgent agent, QueryTranslator translator, ConcurrentLinkedQueue<ActiveCheckResult> responseQueue){
		this.agentHost = agentHost;
		this.item = item;
		this.agent = agent;
		this.translator = translator;
		this.responseQueue = responseQueue;
	}
	
	@Override
	public void run() {
		String key = item.getKey();
		log.debug(ZorkaLogger.ZAG_DEBUG, "Running task: " + key);
		
		String expr = translator.translate(key);
		log.debug(ZorkaLogger.ZAG_DEBUG, "Translated task: " + expr);
		
		clock = (new Date()).getTime() / 1000L;
		agent.exec(expr, this);
	}
	
	@Override
	public String getReq() throws IOException {
		return null;
	}
	
	@Override
	public void handleResult(Object rslt) {
		String key = item.getKey();
		String value = serialize(rslt);
		log.debug(ZorkaLogger.ZAG_DEBUG, "Task response: " + key + " -> " + value);
		
		if (!value.equals(ZabbixActiveRequest.ZBX_NOTSUPPORTED)) { // TODO  && !value.equals("{\"data\":[]}")
			ActiveCheckResult response = new ActiveCheckResult();
			response.setHost(agentHost);
			response.setKey(key);
			response.setValue(value);
			response.setLastlogsize(0);
			response.setClock(clock);
			
			responseQueue.offer(response);
			log.debug(ZorkaLogger.ZAG_DEBUG, "Cache size: " + responseQueue.size());
		}
	}
	
	private String serialize(Object obj) {
        return obj != null ? obj.toString() : ZabbixActiveRequest.ZBX_NOTSUPPORTED;
    }
	
	@Override
	public void handleError(Throwable e) {
		//
	}

}
