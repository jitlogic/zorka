package com.jitlogic.zorka.core.integ;

import com.jitlogic.netkit.*;
import com.jitlogic.netkit.http.*;
import com.jitlogic.netkit.tls.TlsContextBuilder;
import com.jitlogic.zorka.common.ZorkaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.util.ZorkaConfig.*;

public class HttpService implements ZorkaService {

    private static final Logger log = LoggerFactory.getLogger(HttpService.class);

    private volatile List<UrlEndpoint> endpoints = Collections.EMPTY_LIST;

    private HttpConfig httpconf = new HttpConfig();
    private NetServer server;
    private BufHandlerFactory bufHandlerFactory;
    private SSLContext sslContext = null;

    private String prefix;
    private int listenPort;
    private String listenAddr;
    private boolean tlsEnabled;


    public HttpService(String prefix, Map<String,String> config) {
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

        log.info( (tlsEnabled ? "HTTPS" : "HTTP") + " service listening on port: " + listenPort);

        bufHandlerFactory = new BufHandlerFactory() {
            @Override
            public BufHandler create(SocketChannel ch) {
                return new HttpProtocolHandler(httpconf, new UriDispatcher(httpconf, endpoints));
            }
        };

        try {
            server = new NetServer("ZORKA-http-" + prefix,
                    listenAddr, listenPort, bufHandlerFactory, sslContext);
        } catch (IOException e) {
            log.error("Cannot start HTTP service '" + prefix + "'", e);
        }
    }

    public void start() {
        server.start();
    }

    public synchronized void addEndpoint(UrlEndpoint endpoint) {
        List<UrlEndpoint> ep = new ArrayList<UrlEndpoint>(endpoints.size()+1);
        ep.addAll(endpoints);
        ep.add(endpoint);
        endpoints = ep;
    }

    public synchronized void clearEndpoints() {
        endpoints = Collections.EMPTY_LIST;
    }

    @Override
    public void shutdown() {
        server.stop(1000);
    }
}
