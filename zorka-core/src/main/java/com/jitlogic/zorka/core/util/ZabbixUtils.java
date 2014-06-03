package com.jitlogic.zorka.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.GsonBuilder;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.integ.ZabbixActiveRequest;
import com.jitlogic.zorka.core.model.AgentData;
import com.jitlogic.zorka.core.model.Data;

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
	private static String _ACTIVECHECK_MSG = "{ \"request\":\"active checks\", \"host\":\"<hostname>\" }";

	private static String _HOSTNAME_TAG = "<hostname>";


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
	public static String createActiveCheck(String host) {
		return _ACTIVECHECK_MSG.replace(_HOSTNAME_TAG, host);
	}
	
	/**
	 * Creates an Agent Data String Message
	 * @param listData
	 * @param clock
	 */
	public static String createAgentData(ArrayList<Data> listData, long clock) {
		AgentData agentData = new AgentData();
		
		agentData.setRequest("agent data");
		agentData.setData(listData);
		agentData.setClock(clock);
		
		return (new GsonBuilder().disableHtmlEscaping().create()).toJson(agentData);
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
