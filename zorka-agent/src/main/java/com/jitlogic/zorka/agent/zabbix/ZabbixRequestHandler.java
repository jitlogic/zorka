/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.zabbix;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.jitlogic.zorka.agent.ZorkaCallback;
import com.jitlogic.zorka.util.ZorkaLogger;

public class ZabbixRequestHandler implements ZorkaCallback {

	private static final ZorkaLogger log = ZorkaLogger.getLogger(ZabbixRequestHandler.class);
	
	private Socket socket;
	private String req = null;
	
	public static final String ZBX_NOTSUPPORTED = "ZBX_NOTSUPPORTED";
	
	public ZabbixRequestHandler(Socket socket) {
		this.socket = socket;
	}
	
	
	private static final byte[] header = { 0x5a, 0x42, 0x58, 0x44, 0x01 };
	private static final int HDR_LEN = 13;	
	private static final int MAX_REQUEST_LENGTH = 1024;
	
	
	public static String decode(InputStream in) throws IOException {
		byte[] buf = new byte[MAX_REQUEST_LENGTH+HDR_LEN];  
		int pos = 0;
		
		
		for (int b = in.read(); b != -1 && pos < buf.length; b = in.read()) {
			buf[pos++] = (byte)b;
			if (b == 0x0a) {
				break;
			}
		}
		
		boolean hasHdr = true;
		
		if (buf.length > 5) {
			for (int i = 0; i < header.length; i++) {
				if (buf[i] != header[i]) {
					hasHdr = false;
				}
			}
		}
		
		if (hasHdr) {
			while (pos < HDR_LEN) {
				int b = in.read();
				if (b == -1) { 
					return null;
				}
				buf[pos++] = (byte)b;
			}
			
			long len = 0;
			
			for (int i = 0; i < 8; i++) {
				len |= ((long)buf[i+5]) << (i*8);
			}
			
			if (len > MAX_REQUEST_LENGTH) {
				return null;
			}
			
			while (pos < len+HDR_LEN) {
				int b = in.read();
				if (b == -1) {
					return null;
				}
				buf[pos++] = (byte)b;
			}
		}
		
		if (buf[pos-1] == 0x0a) {
			pos--;
		}
		
		StringBuffer sb = new StringBuffer(pos);
		
		for (int i = hasHdr?13:0 ; i < pos; i++) {
			sb.append((char)buf[i]);
		}
		
		return sb.toString();
	}
	
	
	private static final byte[] zbx_hdr = { (byte)'Z', (byte)'B', (byte)'X', (byte)'D', 0x01 };
	
	
	public static String translate(String query) {
		StringBuilder sb = new StringBuilder(query.length());
		int pos = 0;
		
		while (pos < query.length() && query.charAt(pos) != '[') {
			pos++;
		}
		
		sb.append(query.substring(0, pos).replace("__", "."));
		
		if (pos >= query.length()) {
			return sb.toString();
		}
		
		sb.append('('); pos++;
				
		while (pos < query.length() && query.charAt(pos) != ']') {
			if (query.charAt(pos) == '"') {
				int pstart = pos++;
				while (pos < query.length() && query.charAt(pos) != '"') {
					pos++;
				}
				sb.append(query.substring(pstart, pos+1));
			} else {
				sb.append(query.charAt(pos));
			}
			pos++;
		}
		
		sb.append(')');
		
		return sb.toString();
	}
		
	
	private void send(String resp) throws IOException {
		byte[] buf = new byte[resp.length()+zbx_hdr.length+8];
		
		for (int i = 0; i < zbx_hdr.length; i++) { 
			buf[i] = zbx_hdr[i];
		}
		
		long len = resp.length();
		
		for (int i = 0; i < 8; i++) {
			buf[i+zbx_hdr.length] = (byte)(len & 0xff);
			len >>= 8;
		}
		
		for (int i = 0 ; i < resp.length(); i++) {
			buf[i+zbx_hdr.length+8] = (byte)resp.charAt(i);
		}
		
		OutputStream out = socket.getOutputStream();
		out.write(buf); out.flush();
	} // send()
	
	
	public String getReq() throws IOException {
		if (req == null) {
			String s = decode(socket.getInputStream());
			log.debug("Incoming ZABBIX query: '" + s + "'"); // TODO avoid concatenation when log level > 0 (? on ZorkaLogger level ?)
			req = translate(s);
		}
		return req;
	} // getReq()

	
	public void handleResult(Object rslt) {
		try {
			send(rslt != null ? rslt.toString() : ZBX_NOTSUPPORTED);
			socket.close();
		} catch (IOException e) {
			log.error("I/O error returning result: " + e.getMessage());
		}
	}
	
	
	public void handleError(Throwable e) {
		try {
			log.error("Error processing request", e);
			send(ZBX_NOTSUPPORTED);
			socket.close();
		} catch (IOException e1) {
			log.error("I/O Error returning (error) result: " + e.getMessage());
		}
	}
}
