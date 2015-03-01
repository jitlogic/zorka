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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

public final class ZabbixUtils {
	/**
	 * Logger
	 */
	private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixUtils.class);
	
	/**
	 * Message header 
	 */
	private static final byte[] _ZBX_HDR = {(byte) 'Z', (byte) 'B', (byte) 'X', (byte) 'D', 0x01};

	/**
	 * Zabbix header length
	 */
	private static final int _HDR_LEN = 5;
	private static final int _LEN_LEN = 8;

	
	/**
	 * Maximum request length
	 */
	private static int MAX_REQUEST_LENGTH = 16384;

	
	/**
	 * Active Check constants
	 */
	private static String _ACTIVECHECK_MSG = "{ \"request\":\"active checks\", \"host\":\"<hostname>\", \"host_metadata\": \"<host_metadata>\"}";

	private static String _HOSTNAME_TAG = "<hostname>";
	private static String _HOSTMETADATA_TAG = "<host_metadata>";


	/**
	 * Compiled pattern to extract "keys" from string requests  
	 */
	private static Pattern _PATTERN = Pattern.compile("\"key\"\\s*:\\s*\"([^\\]]+])");


	public static void setMaxRequestLength(int max) {
		MAX_REQUEST_LENGTH = max;
	}
	
	
	/**
	 * Creates a Zabbbix message with a "ZBXD\x01" + message.len()[8 byte] + message 
	 * @param msg
	 * @return
	 */
	public static byte[] zbx_format(String msg) {

		byte[] buf = new byte[ZabbixUtils._ZBX_HDR.length + 8 + msg.length()];

		for (int i = 0; i < ZabbixUtils._ZBX_HDR.length; i++) {
			buf[i] = ZabbixUtils._ZBX_HDR[i];
		}

		long len = msg.length();
		log.debug(ZorkaLogger.ZAG_DEBUG, "Message: '" + msg + "'");
		log.debug(ZorkaLogger.ZAG_DEBUG, "Message length: " + len);

		for (int i = 0; i < 8; i++) {
			buf[i + ZabbixUtils._ZBX_HDR.length] = (byte) (len & 0xff);
			len >>= 8;
		}

		for (int i = 0; i < msg.length(); i++) {
			buf[i + ZabbixUtils._ZBX_HDR.length + 8] = (byte) msg.charAt(i);
		}

		return buf.clone();
	}


	/**
	 * Creates a complete Active Check Message
	 * @param host
	 * @return
	 */
	public static String createActiveCheck(String host, String hostMetadata) {
		String res = _ACTIVECHECK_MSG.replace(_HOSTNAME_TAG, host);
		return res.replace(_HOSTMETADATA_TAG, hostMetadata);
	}


	/**
	 * Creates an Agent Data String Message
	 * @param results
	 * @param clock
	 */
	public static String createAgentData(ArrayList<ActiveCheckResult> results, long clock) {
		ActiveCheckQuery query = new ActiveCheckQuery();
		
		query.setRequest("agent data");
		query.setData(results);
		query.setClock(clock);

        return new JSONWriter(false).write(query);
	}
	

	/**
	 * Receives and decodes zabbix request
	 *
	 * @param in input stream (from connection socket)
	 * @return query string
	 * @throws IOException if I/O error occurs
	 */
	public static String decode(InputStream in) throws IOException {
    	byte[] buf = new byte[MAX_REQUEST_LENGTH + _HDR_LEN + _LEN_LEN];
        int pos = 0;

        /* read header */
        for (; pos < _HDR_LEN && pos < buf.length; pos++) {
        	int b = in.read();
        	if (b == -1){
        		return null;
        	}
            buf[pos] = (byte) b;
        }

        /* verify header */
        boolean hasHdr = true;

        if (buf.length > 5) {
            for (int i = 0; i < _ZBX_HDR.length; i++) {
                if (buf[i] != _ZBX_HDR[i]) {
                    hasHdr = false;
                }
            }
        }

        
        /* read length */
        if (hasHdr) {
        	long len = 0;
        	
        	for (int i = 0; i < _LEN_LEN; i++) {
        		int b = in.read();
                if (b == -1) {
                    return null;
                }
                buf[pos++] = (byte) b;
                
                len += (long) ((long) b) << (i * 8);
            }
        	
            if (len > MAX_REQUEST_LENGTH) {
                return null;
            }

            while (pos < len + (_HDR_LEN + _LEN_LEN)) {
                int b = in.read();
                if (b == -1) {
                    return null;
                }
                buf[pos++] = (byte) b;
            }
        }

        if (buf[pos - 1] == 0x0a) {
            pos--;
        }

        StringBuffer sb = new StringBuffer(pos);

        for (int i = hasHdr ? (_HDR_LEN + _LEN_LEN) : 0; i < pos; i++) {
            sb.append((char) buf[i]);
        }

        return sb.toString();
	}


	/**
	 * Extract metrics configuration from message string
	 * @param msg
	 * @return
	 */
	public static ArrayList<String> findKeys(String msg){
		String msg_copy = msg;
		ArrayList<String> keys = new ArrayList<String>();

		Matcher m = _PATTERN.matcher(msg_copy);
		while (m.find( )) {
			keys.add(m.group(1));
		}

		return keys;
	}

}
