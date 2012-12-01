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

import com.jitlogic.zorka.util.Base64;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ZabbixTrapper implements Runnable {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private volatile boolean running = false;
    private Thread thread = null;

    private String serverAddr = null;
    private int serverPort = 10051;
    private String defaultHost;

    private LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>(1024);


    public ZabbixTrapper(String serverAddr, String defaultHost) {
        try {
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

        try {
            sendQueue.offer(sb.toString(), 1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }


    public void start() {
        if (!running) {
            running = true;
            thread = new Thread(this);
            thread.setName("ZORKA-zabbix-trapper");
            thread.setDaemon(true);
            thread.start();
        }
    }


    public void stop() {
        running = false;
    }


    public void run() {
        while (running) {
            runCycle();
        }

        thread = null;
    }


    private void runCycle() {
        Socket socket = null;
        try {
            String msg = sendQueue.poll(10, TimeUnit.MILLISECONDS);
            if (msg != null) {
                socket = new Socket(serverAddr, serverPort);
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
            }
        } catch (InterruptedException e) {
            // ignore this one
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
    } // runCycle()
}
