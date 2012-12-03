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
package com.jitlogic.zorka.integ.syslog;

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Minimal syslog sender implementation.
 */
public class SyslogTrapper implements Runnable {

    public final static int DEFAULT_PORT = 514;

    private ZorkaLog log = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

    private InetAddress syslogAddress;
    private int syslogPort = DEFAULT_PORT;

    private String defaultHost;

    private LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<String>(1024);

    private volatile boolean running;
    private DatagramSocket socket = null;
    private Thread thread = null;


    public SyslogTrapper(String syslogServer, String defaultHost) {
        this(syslogServer, defaultHost, false);
    }

    public SyslogTrapper(String syslogServer, String defaultHost, boolean quiet) {

        try {
            if (syslogServer.contains(":")) {
                String[] parts = syslogServer.split(":");
                syslogAddress = InetAddress.getByName(parts[0]);
                syslogPort = Integer.parseInt(parts[1]);
            } else {
                syslogAddress = InetAddress.getByName(syslogServer);
            }

            if (!quiet) {
                log  = ZorkaLogger.getLog(this.getClass());
            }

        } catch (Exception e) {
            if (log != null) {
                log.error("Cannot configure syslog to " + syslogServer, e);
            }
        }

        this.defaultHost = defaultHost;
    }


    public void log(int severity, int facility, String tag, String content) {
        log(severity, facility, defaultHost, tag, content);
    }


    public void log(int severity, int facility, String hostname, String tag, String content) {
        synchronized (sendQueue) {
            try {
                if (running) {
                    String s = format(severity, facility, new Date(), hostname, tag, content);
                    sendQueue.offer(s, 1, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) { }
        }
    }


    public String format(int severity, int facility, Date date, String hostname, String tag, String content) {
        return "<" + (severity+facility*8) + ">" + dateFormat.format(date) + " "
                + hostname + " " + tag + " " + ZorkaUtil.printableASCII7(content);
    }


    public void start() {
        if (thread == null) {
            try {
                socket = new DatagramSocket();
                thread = new Thread(this);
                thread.setName("ZORKA-syslog-sender");
                thread.setDaemon(true);

                running = true;
                thread.start();
            } catch (SocketException e) {
                if (log != null) {
                    log.error("Cannot open UDP socket", e);
                }
            }
        }
    }


    public void stop() {
        running = false;
    }


    public void run() {

        while (running) {
            runCycle();
        }

        // Shut down thread, socket etc.
        socket.close();
        socket = null;
        thread = null;
    }

    private void runCycle() {
        String msg = null;
        try {
            msg = sendQueue.poll(1, TimeUnit.MILLISECONDS);
            if (msg != null) {
                byte[] buf = msg.getBytes();
                try {
                    socket.send(new DatagramPacket(buf, 0, buf.length, syslogAddress, syslogPort));
                } catch (IOException e) {
                    if (log != null) {
                        log.error("Cannot send syslog packet: " + msg);
                    }
                }
            }
        } catch (InterruptedException e) { }
    }
}
