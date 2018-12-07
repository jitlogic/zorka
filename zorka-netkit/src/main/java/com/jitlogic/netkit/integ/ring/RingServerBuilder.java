package com.jitlogic.netkit.integ.ring;

import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.BufHandlerFactory;
import com.jitlogic.netkit.NetServer;
import com.jitlogic.netkit.TlsUtils;
import com.jitlogic.netkit.http.HttpConfig;
import com.jitlogic.netkit.http.HttpProtocol;
import com.jitlogic.netkit.http.HttpProtocolHandler;
import com.jitlogic.netkit.util.NetkitUtil;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jitlogic.netkit.util.NetkitUtil.*;

public class RingServerBuilder {

    public static final Keyword IP                   = Keyword.intern("ip");
    public static final Keyword PORT                 = Keyword.intern("port");
    public static final Keyword WORKER_THREADS       = Keyword.intern("worker-threads");
    public static final Keyword IOTHREAD_NAME_PREFIX = Keyword.intern("iothread-name-prefix");
    public static final Keyword WORKER_NAME_PREFIX   = Keyword.intern("worker-name-prefix");
    public static final Keyword QUEUE_SIZE           = Keyword.intern("queue-size");
    public static final Keyword MAX_BODY             = Keyword.intern("max-body");
    public static final Keyword MAX_LINE             = Keyword.intern("max-line");
    public static final Keyword KEEP_ALIVE           = Keyword.intern("keep-alive");
    public static final Keyword HANDLER              = Keyword.intern("handler");

    public static final Keyword TLS                  = Keyword.intern("tls?");
    public static final Keyword KEYSTORE             = Keyword.intern("keystore");
    public static final Keyword KEYSTORE_PASS        = Keyword.intern("keystore-pass");

    public static NetServer server(IPersistentMap config) throws IOException {
        String ip = coerceStr(config.valAt(IP, "0.0.0.0"), HttpProtocol.RE_IPV4_ADDR);
        int port = coerceInt(config.valAt(PORT, 8090));
        int nthreads = coerceInt(config.valAt(WORKER_THREADS, 4));

        String iothread = coerceStr(config.valAt(IOTHREAD_NAME_PREFIX, "netkit-io"), null);

        // TODO dedykowany excutor - obsłużyć WORKER_NAME_PREFIX i QUEUE_SIZE

        final IFn fn = NetkitUtil.coerceObj(config.valAt(HANDLER), IFn.class, "Invalid handler function");

        final ExecutorService executor = Executors.newFixedThreadPool(nthreads);

        final HttpConfig htconfig = new HttpConfig();
        htconfig.setKeepAliveTimeout(coerceInt(config.valAt(KEEP_ALIVE, HttpConfig.KEEP_ALIVE)));
        htconfig.setMaxBodySize(coerceInt(config.valAt(MAX_BODY, HttpConfig.MAX_BODY_SIZE)));
        htconfig.setMaxLineSize(coerceInt(config.valAt(MAX_LINE, HttpConfig.MAX_LINE_SIZE)));

        SSLContext sslctx = null;

        if (coerceBool(config.valAt(TLS, false))) {
            sslctx = TlsUtils.svrContext(
                    coerceStr(config.valAt(KEYSTORE, "ssl.jks"), null),
                    coerceStr(config.valAt(KEYSTORE_PASS, "changeit"), null));
        }

        BufHandlerFactory factory = new BufHandlerFactory() {
            @Override
            public BufHandler create(SocketChannel ch) {
                return new HttpProtocolHandler(htconfig,
                        new RingHandler(htconfig, fn, executor));
            }
        };

        return new NetServer(iothread, ip, port, factory, sslctx);
    }

}
