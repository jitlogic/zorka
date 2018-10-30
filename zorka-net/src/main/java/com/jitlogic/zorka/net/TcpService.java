/*
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.net;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class TcpService implements Runnable, ZorkaService {

    private static final Logger log = LoggerFactory.getLogger(TcpService.class);

    private boolean tlsEnabled;
    private int listenPort;
    private InetAddress listenAddr;

    private int defaultPort;
    private String defaultAddr;

    private String prefix;
    private ZorkaConfig config;
    private Executor executor;
    private TcpSessionFactory sessionFactory;

    private ServerSocket socket;
    private ServerSocketFactory socketFactory = null;

    private volatile Thread thread;
    private volatile boolean running = false;
    private List<InetAddress> allowedAddrs = new ArrayList<InetAddress>();


    public TcpService(ZorkaConfig config, Executor executor, TcpSessionFactory sessionFactory,
                      String prefix, String defaultAddr, int defaultPort) {
        this.prefix = prefix;
        this.config = config;
        this.executor = executor;
        this.sessionFactory = sessionFactory;
        this.defaultAddr = defaultAddr;
        this.defaultPort = defaultPort;
    }


    private void setup() {
        String la = config.stringCfg(prefix + ".listen.addr", defaultAddr);
        try {
            listenAddr = InetAddress.getByName(la.trim());
        } catch (UnknownHostException e) {
            log.error("Cannot parse " + prefix + ".listen.addr in zorka.properties", e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        }

        listenPort = config.intCfg(prefix + ".listen.port", defaultPort);

        log.info("Zorka will listen for '" + prefix + "' connections on " + listenAddr + ":" + listenPort);

        for (String sa : config.listCfg(prefix + ".server.addr", "127.0.0.1")) {
            try {
                log.info("Zorka will accept '" + prefix + "' connections from '" + sa.trim() + "'.");
                allowedAddrs.add(InetAddress.getByName(sa.trim()));
            } catch (UnknownHostException e) {
                log.error("Cannot parse " + prefix + ".server.addr in zorka.properties", e);
            }
        }

        tlsEnabled = config.boolCfg(prefix + ".tls", false);
        if (tlsEnabled) setupTls();
    }


    private void setupTls() {
        String keyStorePath = config.stringCfg(prefix + ".tls.keystore",
                new File(config.getHomeDir(), "zorka.jks").getPath());
        String keyStorePass = config.stringCfg(prefix + ".tls.keystore.pass", "changeit");

        File keyStoreFile = new File(keyStorePath);
        if (!keyStoreFile.exists()) {
            log.error("Cannot initialize TLS: file " + keyStorePath + " is missing. Service " + prefix + " will not start.");
            return;
        }

        InputStream is = null;
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            is = new FileInputStream(keyStoreFile);
            keystore.load(is, keyStorePass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, keyStorePass.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keystore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            socketFactory = ctx.getServerSocketFactory();
        } catch (Exception e) {
            log.error("Cannot load TLS key", e);
        } finally {
            ZorkaUtil.close(is);
        }
    }


    /**
     * Opens socket and starts agent thread.
     */
    public void start() {
        if (!running) {
            try {
                socket = createSocket();
                running = true;
                thread = new Thread(this);
                thread.setName("ZORKA-" + prefix + "-main");
                thread.setDaemon(true);
                thread.start();
                log.info("ZORKA-" + prefix + " is listening at " + listenAddr + ":" + listenPort + ".");
            } catch (IOException e) {
                log.error("I/O error while starting " + prefix + " core:" + e.getMessage());
            }
        }
    }

    private ServerSocket createSocket() throws IOException {
        return socketFactory != null
                ? socketFactory.createServerSocket(listenPort, 0, listenAddr)
                : new ServerSocket(listenPort, 0, listenAddr);
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

                log.warn("ZORKA-" + prefix + " thread didn't stop after 1000 milliseconds. Shutting down forcibly.");

                synchronized (this) {
                    thread.stop();
                    thread = null;
                }
            } catch (IOException e) {
                log.error("I/O error in zabbix core main loop: " + e.getMessage());
            }
        }
    }


    public void restart() {
        setup();
        start();
    }


    @Override
    public void shutdown() {
        log.info("Shutting down " + prefix + " agent ...");
        stop();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
    }


    @Override
    public void run() {
        while (running) {
            Socket sock;
            try {
                sock = socket.accept();
                if (!allowedAddr(sock)) {
                    log.warn("Illegal connection attempt from '" + socket.getInetAddress() + "'.");
                    sock.close();
                } else {
                    Runnable r = sessionFactory.getSession(sock);
                    if (r != null) {
                        executor.execute(r);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("Error occured when handling " + prefix + " connection", e);
                }
            }
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
