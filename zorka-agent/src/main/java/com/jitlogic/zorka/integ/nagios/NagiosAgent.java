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
package com.jitlogic.zorka.integ.nagios;

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

public class NagiosAgent implements Runnable {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private volatile ZorkaBshAgent agent;

    private String listenAddr;
    private int listenPort = 10055;

    private InetAddress serverAddr = null;

    private volatile Thread thread;
    private volatile boolean running = false;

    private ServerSocket socket;

    public NagiosAgent(ZorkaBshAgent agent) {
        Properties props = ZorkaConfig.getProperties(); // TODO make constructor argument from it
        this.listenAddr = props.getProperty("nagios.listen.addr", "0.0.0.0");
        this.agent = agent;

        try {
            listenPort = Integer.parseInt(props.getProperty("nagios.listen.port", "5669"));
        } catch (Exception e) {
            log.error("Invalid 'nagios.listen.port' setting in zorka.properties file. Was '" +
                    props.getProperty("nagios.listen.port", "5669") + "', should be integer.");
        }

        try {
            String sa = props.getProperty("nagios.server.addr", "127.0.0.1");
            log.info("Zorka will accept connections from '" + sa.trim() + "'.");
            serverAddr = InetAddress.getByName(sa.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse nagios.server.addr in zorka.properties", e);
        }
    }

    public void start() {
        if (!running) {
            try {
                InetAddress iaddr = InetAddress.getByName(listenAddr);
                socket = new ServerSocket(listenPort, 0, iaddr);
                running = true;
                thread = new Thread(this);
                thread.setName("ZORKA-nagios-main");
                thread.setDaemon(true);
                thread.start();
                log.info("ZORKA-nagios agent is listening at " + listenAddr + ":" + listenPort + ".");
                running = true;
            } catch (UnknownHostException e) {
                log.error("Error starting ZABBIX agent: unknown address: '" + listenAddr + "'");
            } catch (IOException e) {
                log.error("I/O error while starting nagios agent:" + e.getMessage());
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
                    if (thread == null) return;
                }

                log.warn("WARNING: ZORKA-nagios-main thread didn't stop " +
                        "after 1000 milliseconds. Shutting down forcibly.");
                thread.stop();
                thread = null;
            } catch (IOException e) {
                log.error("I/O error in nagios agent main loop: " + e.getMessage());
            }
        }
    }

    public void run() {
        while (running) {
            Socket sock = null;
            NrpeRequestHandler rh = null;
            try {
                sock = socket.accept();
                if (!sock.getInetAddress().equals(serverAddr)) {
                    log.warn("Illegal connection attempt from '" + sock.getInetAddress() + "'.");
                    sock.close();
                    continue;
                }
                rh = new NrpeRequestHandler(sock);
                agent.exec(rh.getReq(), rh);
                sock = null;
            } catch (Exception e) {
                if (running) {
                    log.error("Error occured when processing request.", e);
                }
                if (running && rh != null)
                    rh.handleError(e);
            } finally {
                rh = null; sock = null;
            }

        }

        thread = null;
    }
}
