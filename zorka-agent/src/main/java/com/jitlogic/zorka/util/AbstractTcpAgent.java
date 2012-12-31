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
package com.jitlogic.zorka.util;

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.integ.ZorkaRequestHandler;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Abstract class that implements basic functionality of a service
 * listening on TCP port, handling TCP connections and processing
 * requests using ZorkaBshAgent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class AbstractTcpAgent implements Runnable {

    /** Logger */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** BSH agent */
    private ZorkaBshAgent agent;

    /** Connections accepting thread */
    private Thread thread;

    /** Thread main loop will run as long as this attribute is true */
    private volatile boolean running = false;

    /** Name prefix (will appear in thread name, configuration properties will start with this prefix etc.) */
    private String prefix;

    /** TCP listen port */
    private int listenPort;

    /** TCP listen address */
    private InetAddress listenAddr;

    /** Agent will only accept connections coming from this address. */
    private InetAddress serverAddr;

    /** TCP server socket */
    private ServerSocket socket;

    /**
     * Standard constructor
     * @param agent BSH agent
     * @param props configuration properties
     * @param prefix agent name prefix
     * @param defaultPort agent default port
     */
    public AbstractTcpAgent(ZorkaBshAgent agent, Properties props, String prefix, int defaultPort) {

        this.agent = agent;
        this.prefix = prefix;

        String la = props.getProperty(prefix+".listen.addr");
        try {
            listenAddr = InetAddress.getByName(la.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse "+prefix+".listen.addr in zorka.properties", e);
        }

        String lp = props.getProperty(prefix+".listen.port");
        try {
            listenPort = Integer.parseInt(lp);
        } catch (NumberFormatException e) {
            log.error("Invalid '"+prefix+".listen.port' setting in zorka.properties file. Was '" +
                    lp + "', should be integer.");
        }

        log.info("Zorka will listen for "+prefix+" connections on " + listenAddr + ":" + listenPort);

        String sa = props.getProperty(prefix+".server.addr");
        try {
            log.info("Zorka will accept "+prefix+" connections from '" + sa.trim() + "'.");
            serverAddr = InetAddress.getByName(sa.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse "+prefix+".server.addr in zorka.properties", e);
        }

    }

    /**
     * Opens socket and starts agent thread.
     */
    public void start() {
        if (!running) {
            try {
                socket = new ServerSocket(listenPort, 0, listenAddr);
                running = true;
                thread = new Thread(this);
                thread.setName("ZORKA-"+prefix+"-main");
                thread.setDaemon(true);
                thread.start();
                log.info("ZORKA-"+prefix+" agent is listening at " + listenAddr + ":" + listenPort + ".");
            } catch (IOException e) {
                log.error("I/O error while starting "+prefix+" agent:" + e.getMessage());
            }
        }
    }


    /**
     * Stops agent thread and closes socket.
     */
    @SuppressWarnings("deprecation")
    public void stop() {
        if (running) {
            running = false;
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                for (int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) { }
                    if (thread == null) {
                        return;
                    }
                }

                log.warn("WARNING: ZORKA-main thread didn't stop " +
                        "after 1000 milliseconds. Shutting down forcibly.");
                thread.stop();
                thread = null;
            } catch (IOException e) {
                log.error("I/O error in zabbix agent main loop: " + e.getMessage());
            }
        }
    }

    /**
     * This abstract method creates new request handlers for accepted TCP connections.
     *
     * @param sock socket representing accepted connection
     *
     * @return request handler for new connection
     */
    protected abstract ZorkaRequestHandler newRequest(Socket sock);

    @Override
    public void run() {

        while (running) {
            Socket sock;
            ZorkaRequestHandler rh = null;
            try {
                sock = socket.accept();
                if (!sock.getInetAddress().equals(serverAddr)) {
                    log.warn("Illegal connection attempt from '" + socket.getInetAddress() + "'.");
                    sock.close();
                } else {
                    rh = newRequest(sock);
                    agent.exec(rh.getReq(), rh);
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error occured when processing request.", e);
                }
                if (running && rh != null) {
                    rh.handleError(e);
                }
            }

            rh = null;

        }

        thread = null;

    }

}
