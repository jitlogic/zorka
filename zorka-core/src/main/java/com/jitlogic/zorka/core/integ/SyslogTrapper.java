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
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Minimal syslog sender implementation.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SyslogTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    /**
     * Default syslog port
     */
    public final static int DEFAULT_PORT = 514;

    /**
     * Syslog server IP address
     */
    private InetAddress syslogAddress;

    /**
     * Syslog server UDP port
     */
    private int syslogPort = DEFAULT_PORT;

    /**
     * Default hostname
     */
    private String defaultHost;

    /**
     * Default facility
     */
    private int defaultFacility = SyslogLib.F_LOCAL0;

    /**
     * UDP socket used to send packet to syslog server
     */
    private DatagramSocket socket;


    /**
     * Creates new syslog trapper.
     *
     * @param syslogServer    syslog server IP address
     * @param defaultHost     default host name added to syslog messages
     * @param defaultFacility default facility code added to syslog messages
     */
    public SyslogTrapper(String syslogServer, String defaultHost, int defaultFacility) {
        this(syslogServer, defaultHost, defaultFacility, false);
    }


    /**
     * Creates new syslog trapper.
     *
     * @param syslogServer    syslog server IP address
     * @param defaultHost     default host name
     * @param defaultFacility default facility code
     * @param quiet           if true, trapper will not its own errors to zorka logger
     *                        TODO get rid of this 'quiet' feature after refactoring
     */
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

        } catch (Exception e) {
            if (log != null) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Cannot configure syslog to " + syslogServer, e);
            }
        }

        this.defaultHost = defaultHost;
    }


    /**
     * Logs a message.
     *
     * @param severity syslog severity code
     * @param facility syslog facility code
     * @param tag      tag (eg. component name)
     * @param message  log message
     */
    public void log(int severity, int facility, String tag, String message) {
        log(severity, facility, defaultHost, tag, message);
    }


    /**
     * Logs a message.
     *
     * @param severity syslog severity code
     * @param facility syslog facility code
     * @param hostname host name (as syslog protocol mandates)
     * @param tag      tag (eg. component name)
     * @param message  log message
     */
    public void log(int severity, int facility, String hostname, String tag, String message) {
        String s = format(severity, facility, new Date(), hostname, tag, message);
        AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SUBMITTED);
        if (!submit(s)) {
            AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_DROPPED);
        }
    }


    /**
     * Formats syslog message packet.
     *
     * @param severity message severity
     * @param facility facility code
     * @param date     message timestamp
     * @param hostname host name
     * @param tag      tag (eg.component name)
     * @param message  log message
     * @return proper syslog message
     */
    public String format(int severity, int facility, Date date, String hostname, String tag, String message) {
        return "<" + (severity + facility * 8) + ">" + new SimpleDateFormat("MMM dd HH:mm:ss").format(date) + " "
                + hostname + " " + tag + " " + ZorkaUtil.printableASCII7(message);
    }


    @Override
    public void open() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            handleError("Cannot open UDP socket", e);
        }

    }


    @Override
    public void close() {
        socket.close();
        socket = null;
    }


    @Override
    protected void process(List<String> msgs) {
        for (String msg : msgs) {
            byte[] buf = msg.getBytes();
            try {
                socket.send(new DatagramPacket(buf, 0, buf.length, syslogAddress, syslogPort));
                AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SENT);
            } catch (IOException e) {
                if (log != null) {
                    handleError("Cannot send syslog packet: " + msg, e);
                }
            }
        }
    }


    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        if (e == null) {
            log(logLevel.getSeverity(), defaultFacility, tag, msg);
        } else {
            log(logLevel.getSeverity(), defaultFacility, tag, msg + ": " + e.getMessage());
        }
    }
}
