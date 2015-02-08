/**
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
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.jitlogic.zorka.common.util.JSONReader;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

/**
 * Zabbix Active Request is used by Zabbix Active Agent to 
 * send a request to a Zabbix Server and receive a response
 */
public class ZabbixActiveRequest {

	/**
	 * Logger
	 */
	private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixActiveRequest.class);

	/**
	 * Socket with established server connection.
	 */
	private Socket socket;

	/**
	 * Request string
	 */
	private ArrayList<String> reqs = new ArrayList<String>();

	/**
	 * Timestamps of beginning and end of request handling.
	 */
	private volatile long tStart, tStop;

	/**
	 * Value returned if inner agent (BSH) returns null or throws exception.
	 */
	public static final String ZBX_NOTSUPPORTED = "ZBX_NOTSUPPORTED";

	/**
	 * Regular expression to extract "key" groups
	 */
	private static String regex = "\"key\"\\s*:\\s*\"([^\\]]+])";

	/**
	 * Compiled pattern to extract "keys" from string requests  
	 */
	private static Pattern pattern = Pattern.compile(regex);


	/**
	 * Standard constructor
	 *
	 * @param socket open socket (result from ServerSocket.accept())
	 */
	public ZabbixActiveRequest(Socket socket) {
		this.socket = socket;
		this.tStart = System.nanoTime();

		AgentDiagnostics.inc(AgentDiagnostics.ZABBIX_REQUESTS);
	}


	/*
	 * SENDERS
	 */


	/**
	 * Sends message
	 *
	 * @param message response value
	 * @throws IOException if I/O error occurs
	 */
	public void send(String message) throws IOException {
		byte[] buf = ZabbixUtils.zbx_format(message);
		log.debug(ZorkaLogger.ZAG_DEBUG, "Zorka send: " + new String(buf));

		OutputStream out = socket.getOutputStream();
		out.write(buf);
		out.flush();
	} // send()

	/**
	 * Sends Active Message
	 *
	 * @param host destination host
	 * @throws IOException if I/O error occurs
	 */
	public void sendActiveMessage(String host, String hostMetadata) throws IOException {
		String message = ZabbixUtils.createActiveCheck(host, hostMetadata);
		send(message);
	} // send()


	/*
	 * RECEIVERS
	 */

	/**
	 * Get and decode a Zabbix Request
	 */
	public String getReq() throws IOException {
		String s = null;
		if (reqs.isEmpty()) {
			s = ZabbixUtils.decode(socket.getInputStream());
			log.debug(ZorkaLogger.ZAG_DEBUG, "Zorka get:" + s);
		}
		return s;
	} // getReq()

	/**
	 * Get and decode an Active Check response
	 * @return
	 */
	public ActiveCheckResponse getActiveResponse() {
		ActiveCheckResponse response = null;
		if (reqs.isEmpty()) {
			try {
                response = new JSONReader().read(getReq(), ActiveCheckResponse.class);
			} catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error parsing active response", e);
			}
		}
		
		return response;
	}

}
