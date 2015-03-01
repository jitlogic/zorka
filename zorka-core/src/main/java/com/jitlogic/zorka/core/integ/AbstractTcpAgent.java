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

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that implements basic functionality of a service
 * listening on TCP port, handling TCP connections and processing
 * requests using ZorkaBshAgent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class AbstractTcpAgent implements Runnable, ZorkaService {

    /**
     * Logger
     */
    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * BSH agent
     */
    private ZorkaBshAgent agent;

    /**
     * Connections accepting thread
     */
    private Thread thread;

    /**
     * Thread main loop will run as long as this attribute is true
     */
    private volatile boolean running;

    /**
     * Name prefix (will appear in thread name, configuration properties will start with this prefix etc.)
     */
    private String prefix;

    /**
     * TCP listen port
     */
    private int listenPort;

    private String defaultAddr;

    private int defaultPort;

    private ZorkaConfig config;

    /**
     * TCP listen address
     */
    private InetAddress listenAddr;

    /**
     * List of addresses from which agent will accept connections.
     */
    private List<InetAddress> allowedAddrs = new ArrayList<InetAddress>();

    /**
     * TCP server socket
     */
    private ServerSocket socket;

    /**
     * Query translator
     */
    protected QueryTranslator translator;

    /**
     * Standard constructor
     *
     * @param agent       BSH agent
     * @param prefix      agent name prefix
     * @param defaultAddr
     * @param defaultPort agent default port
     */
    public AbstractTcpAgent(ZorkaConfig config, ZorkaBshAgent agent, QueryTranslator translator,
                            String prefix, String defaultAddr, int defaultPort) {

        this.agent = agent;
        this.prefix = prefix;
        this.translator = translator;

        this.defaultPort = defaultPort;
        this.defaultAddr = defaultAddr;
        this.config = config;

        setup();
    }

    protected void setup() {
        String la = config.stringCfg(prefix + ".listen.addr", defaultAddr);
        try {
            listenAddr = InetAddress.getByName(la.trim());
        } catch (UnknownHostException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse " + prefix + ".listen.addr in zorka.properties", e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }

        listenPort = config.intCfg(prefix + ".listen.port", defaultPort);

        log.info(ZorkaLogger.ZAG_ERRORS, "Zorka will listen for " + prefix + " connections on " + listenAddr + ":" + listenPort);

        for (String sa : config.listCfg(prefix + ".server.addr", "127.0.0.1")) {
            try {
                log.info(ZorkaLogger.ZAG_ERRORS, "Zorka will accept " + prefix + " connections from '" + sa.trim() + "'.");
                allowedAddrs.add(InetAddress.getByName(sa.trim()));
            } catch (UnknownHostException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse " + prefix + ".server.addr in zorka.properties", e);
            }
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
                thread.setName("ZORKA-" + prefix + "-main");
                thread.setDaemon(true);
                thread.start();
                log.info(ZorkaLogger.ZAG_CONFIG, "ZORKA-" + prefix + " core is listening at " + listenAddr + ":" + listenPort + ".");
            } catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "I/O error while starting " + prefix + " core:" + e.getMessage());
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
                    } catch (InterruptedException e) {
                    }
                    if (thread == null) {
                        return;
                    }
                }

                log.warn(ZorkaLogger.ZAG_WARNINGS, "ZORKA-" + prefix + " thread didn't stop after 1000 milliseconds. Shutting down forcibly.");

                thread.stop();
                thread = null;
            } catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "I/O error in zabbix core main loop: " + e.getMessage());
            }
        }
    }

    public void restart() {
        setup();
        start();
    }

    @Override
    public void shutdown() {
        log.info(ZorkaLogger.ZAG_CONFIG, "Shutting down " + prefix + " agent ...");
        stop();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
    }

    /**
     * This abstract method creates new request handlers for accepted TCP connections.
     *
     * @param sock socket representing accepted connection
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
                if (!allowedAddr(sock)) {
                    log.warn(ZorkaLogger.ZAG_WARNINGS, "Illegal connection attempt from '" + socket.getInetAddress() + "'.");
                    sock.close();
                } else {
                    rh = newRequest(sock);
                    String req = rh.getReq();
                    agent.exec(req, rh);
                }
            } catch (Exception e) {
                if (running) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error occured when processing request.", e);
                }
                if (running && rh != null) {
                    rh.handleError(e);
                }
            }

            rh = null;

        }

        thread = null;

    }


    private boolean allowedAddr(Socket sock) {

        for (InetAddress addr : allowedAddrs) {
            if (addr.equals(sock.getInetAddress())) {
                return true;
            }
        }

        return false;
    }

}
