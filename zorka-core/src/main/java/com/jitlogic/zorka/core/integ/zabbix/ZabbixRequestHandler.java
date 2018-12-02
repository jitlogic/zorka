/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.integ.ZorkaRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zabbix request handler is used by ZabbixAgent thread to parse queries from zabbix server and format responses.
 * It handles single request, new instance of ZabbixRequestHandler is created for each new request.
 */
public class ZabbixRequestHandler implements ZorkaRequestHandler {

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(ZabbixRequestHandler.class);

    /**
     * Socket with established server connection.
     */
    private Socket socket;

    /**
     * Request string
     */
    private String req;

    private QueryTranslator translator;

    /**
     * Timestamps of beginning and end of request handling.
     */
    private volatile long tStart, tStop;

    /**
     * Value returned if inner agent (BSH) returns null or throws exception.
     */
    public static final String ZBX_NOTSUPPORTED = "ZBX_NOTSUPPORTED";


    /**
     * Standard constructor
     *
     * @param socket open socket (result from ServerSocket.accept())
     */
    public ZabbixRequestHandler(Socket socket, QueryTranslator translator) {
        this.socket = socket;
        this.translator = translator;
        this.tStart = System.nanoTime();

        AgentDiagnostics.inc(AgentDiagnostics.ZABBIX_REQUESTS);
    }


    /**
     * Zabbix protocol header magic number
     */
    private static final byte[] HEADER = {0x5a, 0x42, 0x58, 0x44, 0x01};


    /**
     * Zabbix header length
     */
    private static final int HDR_LEN = 13;


    /**
     * Maximum request length
     */
    private static final int MAX_REQUEST_LENGTH = 1024;


    /**
     * Receives and decodes zabbix request
     *
     * @param in input stream (from connection socket)
     * @return query string
     * @throws IOException if I/O error occurs
     */
    public static String decode(InputStream in) throws IOException {
        byte[] buf = new byte[MAX_REQUEST_LENGTH + HDR_LEN];
        int pos = 0;

        long len = 0;

        boolean hasHdr = true;

        for (int b = in.read(); b != -1 && pos < buf.length; b = in.read()) {
            buf[pos++] = (byte) b;
            if (b == 0x0a) {
                break;
            }
            if (pos == 5) {
                for (int i = 0; i < HEADER.length; i++) {
                    if (buf[i] != HEADER[i]) {
                        hasHdr = false;
                    }
                }

                if (hasHdr) {
                    pos = 0;
                    for (int i = 0; i < 8; i++) {
                        len |= ((long) in.read()) << (i * 8);
                    }
                    if (len > MAX_REQUEST_LENGTH) return null;
                    while (pos < len) {
                        int x = in.read(buf, pos, (int)(len-pos));
                        if (x > 0) {
                            pos += x;
                        } else {
                            return null;
                        }
                    }
                    break;
                } // hasHdr
            } // pos == 5
        } // for

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pos; i++) sb.append((char)buf[i]);
        return sb.toString();
    }

    /**
     * TODO remove either this or above header constant
     */
    private static final byte[] zbx_hdr = {(byte) 'Z', (byte) 'B', (byte) 'X', (byte) 'D', 0x01};


    /**
     * Constructs and sends response
     *
     * @param resp response value
     * @throws IOException if I/O error occurs
     */
    private void send(String resp) throws IOException {
        byte[] buf = new byte[resp.length() + zbx_hdr.length + 8];

        for (int i = 0; i < zbx_hdr.length; i++) {
            buf[i] = zbx_hdr[i];
        }

        long len = resp.length();

        for (int i = 0; i < 8; i++) {
            buf[i + zbx_hdr.length] = (byte) (len & 0xff);
            len >>= 8;
        }

        for (int i = 0; i < resp.length(); i++) {
            buf[i + zbx_hdr.length + 8] = (byte) resp.charAt(i);
        }

        OutputStream out = socket.getOutputStream();
        out.write(buf);
        out.flush();
    } // send()


    @Override
    public String getReq() throws IOException {
        if (req == null) {
            String s = decode(socket.getInputStream());
            req = translator.translate(s);
        }
        return req;
    } // getReq()


    @Override
    public void handleResult(Object rslt) {
        try {
            tStop = System.nanoTime();
            log.debug("OK [t=" + (tStop - tStart) / 1000000L + "ms] '" + req + "' -> '" + rslt + "'");
            AgentDiagnostics.inc(AgentDiagnostics.ZABBIX_TIME, tStop - tStart);
            send(serialize(rslt));
        } catch (IOException e) {
            log.error("I/O error returning result: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("I/O error closing socket.", e);
            }
        }
    }


    /**
     * Serializes response. If it is JSON object, it will be serialized using toJSONString() method.
     * If it is null, ZBX_NOTSUPPORTED will be returned instead. In all other cases toString() method
     * will be used.
     *
     * @param obj arbitrary value
     * @return serialized (string) value
     */
    private String serialize(Object obj) {
        return obj != null ? obj.toString() : ZBX_NOTSUPPORTED;
    }


    @Override
    public void handleError(Throwable e) {
        AgentDiagnostics.inc(AgentDiagnostics.ZABBIX_ERRORS);
        try {
            this.tStop = System.nanoTime();
            log.error("ERROR [t=" + (tStop - tStart) / 1000000L + "ms] + '" + req + "'", e);
            AgentDiagnostics.inc(AgentDiagnostics.ZABBIX_TIME, tStop - tStart);
            send(ZBX_NOTSUPPORTED);
        } catch (IOException e1) {
            log.error("I/O Error returning (error) result: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e2) {
                log.error("I/O error closing socket.", e2);
            }
        }
    }
}
