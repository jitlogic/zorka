package com.jitlogic.zorka.common.http;

import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.TlsContextBuilder;
import com.jitlogic.zorka.common.ZorkaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.jitlogic.zorka.common.util.ZorkaConfig.*;

public class HttpService implements ZorkaService, HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpService.class);

    private volatile Map<String,HttpHandler> endpoints = new ConcurrentHashMap<String, HttpHandler>();

    private HttpConfig httpconf = new HttpConfig();
    private HttpServer server;
    private SSLContext sslContext = null;

    private String prefix;
    private int listenPort;
    private String listenAddr;
    private boolean tlsEnabled;


    public HttpService(String prefix, Map<String,String> config, Executor executor, MethodCallStatistics stats) {
        this.prefix = prefix;
        listenAddr = parseStr(config.get("listen.addr"), HttpProtocol.RE_IPV4_ADDR, "0.0.0.0",
                prefix + ".listen.addr listen address should be in IPv4 form.");
        listenPort = parseInt(config.get("listen.port"), 8641,
                prefix + ".listen.port should be number.");
        tlsEnabled = parseBool(config.get("tls"), false,
                prefix + ".tls should be set to either 'yes' or 'no'");

        if (tlsEnabled) {
            String keystore = parseStr(config.get("keystore"), null, null,
                    prefix + ".keystore should point to .jks file with SSL keystore.");
            String keypass = parseStr(config.get("keystore.pass"), null, "changeit",
                    prefix + ".keypass should contain proper password to keystore.");
            sslContext = TlsContextBuilder.svrContext(keystore, keypass);
        }

        this.server = new HttpServer(prefix, listenAddr, listenPort, new HttpConfig(), this, executor);

        log.info("{} service listening on port: {}", tlsEnabled ? "HTTPS" : "HTTP", listenPort);
    }

    public void start() {
        server.start();
    }

    public synchronized void addEndpoint(String path, HttpHandler handler) {
        log.info("Adding HTTP endpoint: " + path + " -> " + handler);
        endpoints.put(path, handler);
    }

    public synchronized void clearEndpoints() {
        endpoints.clear();
    }

    @Override
    public void shutdown() {
        server.stop();
    }

    @Override
    public HttpMessage handle(HttpMessage message) {
        for (Map.Entry<String,HttpHandler> e : endpoints.entrySet()) {
            if (e.getKey().equals(message.getUri())) {
                System.out.println("k=" + e.getKey() + ", uri=" + message.getUri());
                return e.getValue().handle(message);
            }
        }
        return HttpMessage.RESP(404, "Page not found.");
    }
}
