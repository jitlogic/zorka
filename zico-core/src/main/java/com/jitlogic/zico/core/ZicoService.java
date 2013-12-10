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
package com.jitlogic.zico.core;

import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ZicoService implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(ZicoService.class);

    public final static int COLLECTOR_PORT = 8640;
    public final static int AGENT_PORT = 8642;

    private volatile Thread thread;
    private volatile boolean running;

    /**
     * TCP listen port
     */
    private int listenPort;

    /**
     * TCP listen address
     */
    private InetAddress listenAddr;

    /**
     * TCP server socket
     */
    private ServerSocket socket;

    private ZicoDataProcessorFactory factory;

    private Executor executor;

    private AtomicLong connCnt = new AtomicLong(0);

    public ZicoService(String listenAddr, int listenPort, ZicoDataProcessorFactory factory) {
        this.listenPort = listenPort;
        this.factory = factory;
        executor = Executors.newFixedThreadPool(32); // TODO make this configurable
        try {
            this.listenAddr = InetAddress.getByName(listenAddr);
        } catch (UnknownHostException e) {
            //log.error("Cannot resolve address: " + listenAddr, e);
        }
    }


    @Override
    public void run() {
        while (running) {
            Socket sock;
            try {
                sock = socket.accept();
                connCnt.incrementAndGet();
                ZicoServerConnector handler = new ZicoServerConnector(sock, factory);
                executor.execute(handler);
            } catch (SocketException e) {
                // TODO check if socket closed
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }
        }
    }


    public void start() {
        if (thread == null) {
            try {
                log.info("Starting ZICO service at " + listenAddr + ":" + listenPort);
                socket = new ServerSocket(listenPort, 0, listenAddr);
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.setName("ZICO-acceptor-" + listenPort);
                running = true;
                thread.start();
                log.info("ZICO service started.");
            } catch (IOException e) {
                // TODO Log error here
            }
        }
    }


    public void stop() {
        if (running) {
            try {
                socket.close();
            } catch (IOException e) {

            }
            running = false;
        }
    }

    public long connCnt() {
        return connCnt.get();
    }
}
