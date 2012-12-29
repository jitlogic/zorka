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
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

public abstract class TcpServiceThread implements Runnable {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private ZorkaBshAgent agent;

    private volatile Thread thread;
    private volatile boolean running = false;

    private String prefix;
    private int listenPort;
    private InetAddress serverAddr = null, listenAddr;

    private ServerSocket socket;


    public TcpServiceThread(ZorkaBshAgent agent, Properties props, String prefix, int defaultPort) {

        this.agent = agent;
        this.prefix = prefix;

        try {
            String la = props.getProperty(prefix+".listen.addr", "127.0.0.1");
            listenAddr = InetAddress.getByName(la.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse "+prefix+".server.addr in zorka.properties", e);
        }

        try {
            listenPort = Integer.parseInt(props.getProperty(prefix+".listen.port", ""+defaultPort));
        } catch (Exception e) {
            log.error("Invalid '"+prefix+".listen.port' setting in zorka.properties file. Was '" +
                    props.getProperty(prefix+".listen.port", ""+defaultPort) + "', should be integer.");
        }

        log.info("Zorka will listen for "+prefix+" connections on " + listenAddr + ":" + listenPort);

        try {
            String sa = props.getProperty(prefix+".server.addr", "127.0.0.1");
            log.info("Zorka will accept "+prefix+" connections from '" + sa.trim() + "'.");
            serverAddr = InetAddress.getByName(sa.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse zabbix.server.addr in zorka.properties", e);
        }

    }


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


    protected abstract ZorkaRequestHandler newRequest(Socket sock);


    public void run() {

        while (running) {
            Socket sock = null;
            ZorkaRequestHandler rh = null;
            try {
                sock = socket.accept();
                if (!sock.getInetAddress().equals(serverAddr)) {
                    log.warn("Illegal connection attempt from '" + socket.getInetAddress() + "'.");
                    sock.close();
                    continue;
                }
                rh = newRequest(sock);
                agent.exec(rh.getReq(), rh);
            } catch (Exception e) {
                if (running) {
                    log.error("Error occured when processing request.", e);
                }
                if (running && rh != null) {
                    rh.handleError(e);
                }
            }

            sock = null;
            rh = null;

        } // while (running)

        thread = null;

    } // run()

}
