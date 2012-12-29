/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.integ.zabbix;

import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogLevel;
import com.jitlogic.zorka.logproc.ZorkaLogger;
import com.jitlogic.zorka.logproc.ZorkaTrapper;
import com.jitlogic.zorka.util.*;

import java.io.OutputStream;
import java.net.Socket;

public class ZabbixTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private String serverAddr = null;
    private int serverPort = 10051;
    private String defaultHost, defaultItem;

    public ZabbixTrapper(String serverAddr, String defaultHost, String defaultItem) {
        super("zabbix-trapper");
        try {
            this.defaultItem = defaultItem;
            this.defaultHost = defaultHost;
            if (serverAddr.contains(":")) {
                String[] s = serverAddr.split(":");
                this.serverPort = Integer.parseInt(s[1]);
                this.serverAddr = s[0];
            } else {
                this.serverAddr = serverAddr;
            }
        } catch (Exception e) {
            log.error("Cannot initialize zabbix trapper object.", e);
        }
    }


    public void send(String item, Object value) {
        send(defaultHost, item, value);
    }


    public void send(String host, String item, Object value) {

        StringBuilder sb = new StringBuilder(384);
        sb.append("<req><host>");
        sb.append(Base64.encodeToString(host.getBytes(), false));
        sb.append("</host><key>");
        sb.append(Base64.encodeToString(item.getBytes(), false));
        sb.append("</key><data>");
        sb.append(Base64.encodeToString(value.toString().getBytes(), false));
        sb.append("</data></req>");

        submit(sb.toString());
    }


    @Override
    protected void process(String msg) {
        Socket socket = null;
        try {
            socket = new Socket(serverAddr, serverPort);
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
            os.flush();
        } catch (Exception e) {
            log.error("Error sending ");
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                log.error("Error closing zabbix trapper socket. Open socket may leak.", e);
            }
        }
    }

    protected void handleError(String msg, Throwable e) {
        log.error(msg, e);
    }

    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        send(defaultItem, tag + " " + msg);
    }
}
