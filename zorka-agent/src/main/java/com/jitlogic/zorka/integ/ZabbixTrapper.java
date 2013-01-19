/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.integ;

import com.jitlogic.zorka.util.*;

import java.io.OutputStream;
import java.net.Socket;

/**
 * Zabbix trapper sends traps to zabbix server.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZabbixTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    /** Logger */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Zabbix server IP address */
    private String serverAddr;

    /** Zabbix server TCP port */
    private int serverPort = 10051;

    /** Default host (as presented to zabbix) */
    private String defaultHost;

    /** Default item name (as presented to zabbix) */
    private String defaultItem;


    /**
     * Creates zabbix trapper. Note that in order to actually send traps, it also must be started using start() method.
     *
     * @param serverAddr zabbix server address
     *
     * @param defaultHost default host name (as presented to zabbix)
     *
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
            log.error("Cannot initialize zabbix trapper object.", e);
        }
    }


    /**
     * Sends trap with default host name and default item name.
     *
     * @param value item value
     */
    public void send(Object value) {
        send(defaultHost, value);
    }


    /**
     * Sends trap with default host name and supplied item name.
     *
     * @param item item name
     *
     * @param value item value
     */
    public void send(String item, Object value) {
        send(defaultHost, item, value);
    }


    /**
     * Send trap with supplied host name and item name.
     *
     * @param host host name
     *
     * @param item item name
     *
     * @param value item value
     */
    public void send(String host, String item, Object value) {

        StringBuilder sb = new StringBuilder(384);
        sb.append("<req><host>");
        sb.append(Base64.encode(host.getBytes(), false));
        sb.append("</host><key>");
        sb.append(Base64.encode(item.getBytes(), false));
        sb.append("</key><data>");
        sb.append(Base64.encode(value.toString().getBytes(), false));
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


    /**
     * Logs occuring errors.
     *
     * @param msg error message
     * @param e exception object
     */
    protected void handleError(String msg, Throwable e) {
        log.error(msg, e);
    }


    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        send(tag, msg);
    }
}
