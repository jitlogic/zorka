package com.jitlogic.zorka.core.integ;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.model.ActiveCheckData;
import com.jitlogic.zorka.core.model.Data;

public class ZabbixActiveTask implements Runnable, ZorkaRequestHandler {

	/* Logger */
	private final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveTask.class);
	
	private String agentHost;
	private ActiveCheckData item;
	private ZorkaBshAgent agent;
	private QueryTranslator translator;
	private ConcurrentLinkedQueue<Data> responseQueue;
	
	private long clock;
	
	public ZabbixActiveTask(String agentHost, ActiveCheckData item, ZorkaBshAgent agent, QueryTranslator translator, ConcurrentLinkedQueue<Data> responseQueue){
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
		
		if (!rslt.equals(ZabbixActiveRequest.ZBX_NOTSUPPORTED)) {
			Data response = new Data();
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
