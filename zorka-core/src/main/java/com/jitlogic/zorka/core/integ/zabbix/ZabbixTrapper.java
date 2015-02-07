/**
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

import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.util.*;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Zabbix trapper sends traps to zabbix server.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZabbixTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    /**
     * Logger
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Zabbix server IP address
     */
    private String serverAddr;

    /**
     * Zabbix server TCP port
     */
    private int serverPort = 10051;

    /**
     * Default host (as presented to zabbix)
     */
    private String defaultHost;

    /**
     * Default item name (as presented to zabbix)
     */
    private String defaultItem;


    /**
     * Creates zabbix trapper. Note that in order to actually send traps, it also must be started using start() method.
     *
     * @param serverAddr  zabbix server address
     * @param defaultHost default host name (as presented to zabbix)
     * @param defaultItem default item name (as presented to zabbix)
     */
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot initialize zabbix trapper object.", e);
        }
    }


    /**
     * Sends trap with default host name and default item name.
     *
     * @param value item value
     */
    public boolean send(Object value) {
        return send(defaultHost, value);
    }


    /**
     * Sends trap with default host name and supplied item name.
     *
     * @param item  item name
     * @param value item value
     */
    public boolean send(String item, Object value) {
        return send(defaultHost, item, value);
    }


    /**
     * Send trap with supplied host name and item name.
     *
     * @param host  host name
     * @param item  item name
     * @param value item value
     */
    public boolean send(String host, String item, Object value) {

        StringBuilder sb = new StringBuilder(384);
        sb.append("<req><host>");
        sb.append(Base64.encode(host.getBytes(), false));
        sb.append("</host><key>");
        sb.append(Base64.encode(item.getBytes(), false));
        sb.append("</key><data>");
        sb.append(Base64.encode(value.toString().getBytes(), false));
        sb.append("</data></req>");

        return submit(sb.toString());
    }


    @Override
    protected void process(List<String> msgs) {
        for (String msg : msgs) {
            Socket socket = null;
            try {
                // TODO this really should be sent at once, not every message in separate connection
                socket = new Socket(serverAddr, serverPort);
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
                AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SENT);
            } catch (Exception e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error sending packet", e);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error closing zabbix trapper socket. Possible leak.", e);
                }
            }
        }
    }


    /**
     * Logs occuring errors.
     *
     * @param msg error message
     * @param e   exception object
     */
    protected void handleError(String msg, Throwable e) {
        log.error(ZorkaLogger.ZAG_ERRORS, msg, e);
    }


    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SUBMITTED);
        if (!send(tag, msg)) {
            AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_DROPPED);
        }
    }
}
