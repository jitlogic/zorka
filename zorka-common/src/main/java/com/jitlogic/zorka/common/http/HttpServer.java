package com.jitlogic.zorka.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executor;

public class HttpServer implements Runnable {

    private static Logger log = LoggerFactory.getLogger(HttpServer.class);

    private volatile boolean running;

    private Thread thread;

    private ServerSocket socket;

    private String name;

    private int listenPort;
    private String listenAddr;

    private HttpHandler listener;

    private Executor executor;

    private HttpConfig config;

    private SSLContext sslContext;

    public HttpServer(String name, String address, int port, HttpConfig config, HttpHandler listener, Executor executor, SSLContext sslContext) {
        this.name = name;
        this.listenAddr = address;
        this.listenPort = port;
        this.listener = listener;
        this.executor = executor;
        this.config = config;
        this.sslContext = sslContext;
    }


    public synchronized void start() {
        if (!running) {
            try {
                if (sslContext != null) {
                    socket = sslContext.getServerSocketFactory().createServerSocket(listenPort, 0, InetAddress.getByName(listenAddr));
                } else {
                    socket = new ServerSocket(listenPort, 0, InetAddress.getByName(listenAddr));
                }
                thread = new Thread(this);
                thread.setName("ZORKA-" + name + "-http");
                thread.setDaemon(true);
                running = true;
                thread.start();
                log.info("ZORKA-{}-http is listening at {}:{}", name, listenAddr, listenPort);
            } catch (IOException e) {
                log.error("I/O error while starting ZORKA-{}-http thread: {}", name, e.getMessage());
            }
        }
    }

    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : 0;
    }

    public synchronized void stop() {
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

                log.warn("ZORKA-{} thread didn't stop after 1000 milliseconds. Shutting down forcibly.", name);

                thread.stop();
                thread = null;
            } catch (IOException e) {
                log.error("I/O error in zabbix core main loop: {}", e.getMessage());
            }
        }

    }

    public void run() {
        while (running) {
            try {
                executor.execute(new HttpServerConnection(config, socket.accept(), listener));
            } catch (Exception e) {
                if (running) {
                    log.error("Error occured when processing request", e);
                }
            }
        }
    }
}
