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

import com.jitlogic.zorka.logproc.ZorkaTrapper;
import com.jitlogic.zorka.util.ZorkaAsyncThread;
import com.jitlogic.zorka.logproc.ZorkaLogLevel;
import com.jitlogic.zorka.logproc.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Minimal syslog sender implementation.
 */
public class SyslogTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    public final static int DEFAULT_PORT = 514;

    private InetAddress syslogAddress;
    private int syslogPort = DEFAULT_PORT;

    private String defaultHost;

    private int defaultFacility = SyslogLib.F_LOCAL0;

    private DatagramSocket socket = null;


    public SyslogTrapper(String syslogServer, String defaultHost, int defaultFacility) {
        this(syslogServer, defaultHost, defaultFacility, false);
    }


    public SyslogTrapper(String syslogServer, String defaultHost, int defaultFacility, boolean quiet) {
        super("syslog-trapper");
        this.defaultFacility = defaultFacility;
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
        String s = format(severity, facility, new Date(), hostname, tag, content);
        submit(s);
    }


    public String format(int severity, int facility, Date date, String hostname, String tag, String content) {
        return "<" + (severity+facility*8) + ">" + new SimpleDateFormat("MMM dd HH:mm:ss").format(date) + " "
                + hostname + " " + tag + " " + ZorkaUtil.printableASCII7(content);
    }


    public void open() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            handleError("Cannot open UDP socket", e);
        }

    }


    public void close() {
        socket.close();
        socket = null;
    }


    @Override
    protected void process(String msg) {
        byte[] buf = msg.getBytes();
        try {
            socket.send(new DatagramPacket(buf, 0, buf.length, syslogAddress, syslogPort));
        } catch (IOException e) {
            if (log != null) {
                handleError("Cannot send syslog packet: " + msg, e);
            }
        }
    }


    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        if (e == null) {
            log(logLevel.getSeverity(), defaultFacility, tag, msg);
        } else {
            log(logLevel.getSeverity(), defaultFacility, tag, msg + ": " + e.getMessage());
        }
    }
}
