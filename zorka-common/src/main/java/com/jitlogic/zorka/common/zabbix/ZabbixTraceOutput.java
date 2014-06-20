/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.zabbix;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.jitlogic.zorka.common.model.Data;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.tracedata.MethodCallCounterRecord;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.PerfRecord;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceOutput;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZabbixUtils;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

/**
 * Tracer output sending data to remote ZICO collector. It automatically handles reconnections and retransmissions,
 * lumps data into bigger packets for better throughput, keeps track of symbols already sent etc.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZabbixTraceOutput extends ZorkaAsyncThread<SymbolicRecord> implements TraceOutput {

	private static ZorkaLog log = ZorkaLogger.getLog(ZabbixTraceOutput.class);

	SymbolRegistry symbolRegistry;
	MetricsRegistry metricsRegistry;

	/**
	 * Hostname this client will advertise itself as 
	 */
	private String hostname;

	/**
	 * Connection Settings
	 */
	private String serverAddr;
	private int serverPort;
	private Socket socket;


	/**
	 * Output buffer -  compatibility purposes
	 */
	private ByteArrayOutputStream os;

	/**
	 * Maximum retransmission retries
	 */
	private int retries;

	/**
	 * Retry wait timing parameters
	 */
	private long retryTime, retryTimeExp;

	/**
	 * Suggested maximum packet size
	 */
	private long packetSize;


	/**
	 * Creates trace output object.
	 * @param metricsRegistry 
	 * @param symbolRegistry 
	 *
	 * @param addr         host name or IP address of remote ZICO collector
	 * @param port         port number of remote ZICO collector
	 * @param hostname     name this client will advertise itself when connecting to ZICO collector
	 * @param qlen         output queue length
	 * @param packetSize   maximum (recommended) packet size (actual packets might exceed this a bit)
	 * @throws IOException when connection to remote server cannot be established;
	 */
	public ZabbixTraceOutput(/*TraceWriter writer, */
			SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, String addr, int port, String hostname, 
			int qlen, long packetSize, int retries, long retryTime, long retryTimeExp, 
			int timeout, int interval) throws IOException {

		super("zabbix-output", qlen, 1, interval);

		log.debug(ZorkaLogger.ZAG_DEBUG, "Configured tracer output: host=" + hostname
				+ ", addr=" + addr 
				+ ", port=" + port
				+ ", qlen=" + qlen 
				+ ", packetSize=" + packetSize
				+ ", interval=" + interval);

		this.symbolRegistry = symbolRegistry;
		this.metricsRegistry = metricsRegistry;

		this.serverAddr = addr;
		this.serverPort = port;
		this.hostname = hostname;

		this.packetSize = packetSize;
		this.retries = retries;
		this.retryTime = retryTime;
		this.retryTimeExp = retryTimeExp;

		/* compatibility purposes */
		this.os = new ByteArrayOutputStream();
	}


	@Override
	public OutputStream getOutputStream() {
		/* compatibility purposes */
		return os;
	}


	@Override
	public boolean submit(SymbolicRecord obj) {
		boolean submitted = false;
		try {
			submitted = submitQueue.offer(obj, 1, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}

		if (!submitted) {
			AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_DROPPED);
		}

		return submitted;
	}


	@Override
	protected void process(List<SymbolicRecord> records) {
		long clock;
		long rt = retryTime;

		ArrayList<Data> listData = new ArrayList<Data>();

		// packet: avoid losing records taken from Queue
		List<SymbolicRecord> packet = new ArrayList<SymbolicRecord>();
		packet.addAll(records);

		for (int i = 0; i < retries; i++) {
			try {
				clock = (new Date()).getTime() / 1000L;

				/* Open Connection */
				close();
				log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Opening connection to " + hostname
						+ " -> " + serverAddr + ":" + serverPort);
				socket = new Socket(serverAddr, serverPort);

				listData.clear();

				for (SymbolicRecord rec : packet) {
					listData.addAll(recToData(rec));
				}

				while (os.size() < packetSize && submitQueue.size() > 0) {
					SymbolicRecord rec = submitQueue.take();
					// save to temp list
					packet.add(rec); 
					listData.addAll(recToData(rec));
				}

				String message = ZabbixUtils.createAgentData(listData, clock);
				byte[] buf = ZabbixUtils.zbx_format(message);

				OutputStream out = socket.getOutputStream();
				out.write(buf);
				out.flush();

				AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_SENT);

				/* Close Connection */
				out.close();
				socket.close();
				return;

			} catch (UnknownHostException e) {
				/* Error caused by unkown host -> failure with no retries */
				log.error(ZorkaLogger.ZCL_STORE, "Error sending trace record: " + e + ". Trace will be lost.");
				AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_LOST);
				return;

			} catch (InterruptedException e) {
				/* Error while taking record from queue */
				log.error(ZorkaLogger.ZCL_STORE, "Error retrieving trace record: " + e + ". Trace will be lost.");
				AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_LOST);
				return;

			} catch (IOException e) {
				/* Error while sending */
				log.error(ZorkaLogger.ZCL_STORE, "Error sending trace record: " + e + ". Resetting connection.");
				close();
				AgentDiagnostics.inc(AgentDiagnostics.ZICO_RECONNECTS);
			}

			/* Wait before retry */
			try {
				log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Will retry (wait=" + rt + ")");
				Thread.sleep(rt);
			} catch (InterruptedException e) {
				log.debug(ZorkaLogger.ZTR_TRACER_DBG, e.getMessage());
			}

			rt *= retryTimeExp;
		} // for ( ... )

		AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_LOST);
		log.error(ZorkaLogger.ZCL_STORE, "Too many errors while trying to send trace. Giving up. Trace will be lost.");
	}


	private ArrayList<Data> recToData(SymbolicRecord rec) {
		/*** Data ***
		 * String host;
		 * String key;
		 * String value;
		 * int lastlogsize;
		 * long clock;
		 */
		ArrayList<Data> list = null;

		if (rec instanceof MethodCallCounterRecord) {
			// TODO Auto-generated method stub
		} else if (rec instanceof PerfRecord) {
			list = perfRecordToData(rec);
		} else if (rec instanceof SymbolicException) {
			// TODO Auto-generated method stub
		} else if (rec instanceof SymbolicStackElement) {
			// TODO Auto-generated method stub
		} else if (rec instanceof TraceMarker) {
			// TODO Auto-generated method stub
		} else if (rec instanceof TraceRecord) {
			list = traceRecordToData(rec, "", 0);
		}

		for (Data data : list){
			log.debug(ZorkaLogger.ZAG_DEBUG, "### Data: " + data.toString());
		}

		return list;
	}


	/*** PerfRecord : 
	 * long clock,
	 * int scannerId,
	 * List<PerfSample> samples [
	 * 	  Metric metricId,
	 *    Number value]
	 */
	private ArrayList<Data> perfRecordToData(SymbolicRecord rec) {
		log.debug(ZorkaLogger.ZAG_DEBUG, "### perfRecordToData");

		ArrayList<Data> list = new ArrayList<Data>();

		Data data;
		PerfRecord perfRecord = (PerfRecord) rec;

		long clock = perfRecord.getClock();

		for (PerfSample sample : perfRecord.getSamples()) {
			data = new Data();

			data.setHost(hostname);
			data.setKey(sample.getMetric().getName());
			data.setValue(String.valueOf(sample.getValue()));
			data.setLastlogsize(0);
			data.setClock(clock);

			list.add(data);
		}

		return list;
	}


	/** TraceRecord : 
	 * int classId,
	 * int methodId,
	 * int signatureId,
	 * int flags,
	 * long time,
	 * long calls,
	 * long errors,
	 * TraceMarker marker,
	 * Object exception,
	 * TraceRecord parent,
	 * Map<Integer, Object> attrs,
	 * List<TraceRecord> children
	 * @param string 
	 */
	private ArrayList<Data> traceRecordToData(SymbolicRecord rec, String prefix, int level) {
		log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData");

		ArrayList<Data> list = new ArrayList<Data>();
		Data data;
		String keySuffix = null; 
		TraceRecord traceRecord = (TraceRecord) rec;
		long clock = traceRecord.getClock() / 1000l;
		
		/* Finding Record's key */
		if (traceRecord.getAttrs() != null) {
			for (Map.Entry<Integer, Object> entry : traceRecord.getAttrs().entrySet()) {
				String attrName = symbolRegistry.symbolName(entry.getKey());
				
				if (attrName.equals("URI")) {
					keySuffix = "frontends." + String.valueOf(entry.getValue());
				}
			}
		}

		if (keySuffix == null) {
			String className = symbolRegistry.symbolName(traceRecord.getClassId()).replace(".", "_");
			String methodName = symbolRegistry.symbolName(traceRecord.getMethodId()).replace(".", "_");
			keySuffix = className + "_" + methodName;
		}
		
		String key;
		if (prefix == null || prefix.length() == 0) {
			key = keySuffix;
		} else {
			key = prefix + "." + keySuffix;
		}
		log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: key=" + key);
		
		
		/* Time */
		data = new Data();
		data.setHost(hostname);
		data.setKey(key + ".responseTime");
		/* nanoseconds -> milliseconds */
		data.setValue(String.valueOf(traceRecord.getTime() / 1000000l)); 
		data.setLastlogsize(0);
		data.setClock(clock);
		list.add(data);
		log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: data=" + data.toString());

		/* Calls */
		data = new Data();
		data.setHost(hostname);
		data.setKey(key + ".count");
		 /* contar a chamada atual como 1 */
		data.setValue("1");
		data.setLastlogsize(0);
		data.setClock(clock);
		list.add(data);
		log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: data=" + data.toString());

		/* Errors */
		data = new Data();
		data.setHost(hostname);
		data.setKey(key + ".errors.count");
		data.setValue(String.valueOf(traceRecord.getErrors()));
		data.setLastlogsize(0);
		data.setClock(clock);
		list.add(data);
		log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: data=" + data.toString());

		/* Recursive check children */
		if ((level <= 1) && (traceRecord.getChildren() != null)) {
			log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: children.size()=" + traceRecord.getChildren().size());
			for (TraceRecord child : traceRecord.getChildren()) {
				log.debug(ZorkaLogger.ZAG_DEBUG, "### traceRecordToData: child=" + child.toString());
				list.addAll(traceRecordToData(child, key + ".backends", level+1));
			}
		}
		
		return list;
	}

	@Override
	public void open() {
		//        log.debug(ZorkaLogger.ZAG_DEBUG, "Starting network tracer output: " + hostname
		//                + " -> " + serverAddr + ":" + serverPort);
	}

	@Override
	public synchronized void close() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "Closing connection: " + hostname
				+ " -> " + serverAddr + ":" + serverPort);

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

	@Override
	public void start() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "### Start()");
		super.start();
	}

	@Override
	public void run() {
		log.debug(ZorkaLogger.ZAG_DEBUG, "### run()");
		super.run();
	}

}
