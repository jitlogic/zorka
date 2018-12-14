/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.netkit;

import static com.jitlogic.netkit.TlsContext.HANDSHAKE;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.jitlogic.netkit.log.EventSink;
import com.jitlogic.netkit.log.LoggerFactory;

import javax.net.ssl.SSLContext;

public class NetServer extends NetEngine implements Runnable {

    private final EventSink evtAccepts = LoggerFactory.getSink(LoggerFactory.EV_ACCEPTS);

    private final ServerSocketChannel serverChannel;

    public NetServer(String threadName, String ip, int port, final BufHandlerFactory svhFactory, SSLContext sslContext) throws IOException {
        this(threadName, ip, port, new NetCtxFactory() {
            @Override
            public NetCtx create(SocketChannel ch, BufHandler in, BufHandler out) {
                return new NetCtx(ch, svhFactory.create(ch), out);
            }
        }, sslContext);
    }

    public NetServer(String threadName, String ip, int port, NetCtxFactory ctxFactory, SSLContext sslContext) throws IOException {
        super(threadName, ctxFactory, sslContext);

        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(ip, port));
        serverChannel.register(selector, OP_ACCEPT);
    }

    protected void accept(SelectionKey key) {
        ServerSocketChannel ch = (ServerSocketChannel) key.channel();
        SocketChannel s;
        try {
            while ((s = ch.accept()) != null) {
                long t0 = evtAccepts.time();
                s.configureBlocking(false);
                NetCtx atta = ctxFactory.create(s, null,this);
                s.register(selector, OP_READ, atta);
                log.traceNio(key, "accept()", "AFTER");
                if (sslContext != null) {
                    TlsContext tctx = new TlsContext();
                    tctx.setSslEngine(sslContext.createSSLEngine());
                    tctx.getSslEngine().setUseClientMode(false);
                    tctx.getSslEngine().beginHandshake();
                    tctx.setSslState(HANDSHAKE);
                    atta.setTlsContext(tctx);
                    log.traceNio(key, "accept()", "SSL");
                }
                evtAccepts.call(t0);
            }
        } catch (Exception e) {
            // eg: too many open files. do not quit
            log.error("accept incoming request", e);
            evtAccepts.error();
        }
    }

    public void stop(int timeout) {
        try {
            serverChannel.close(); // stop accept any request
        } catch (IOException ignore) {
        }

        // wait all requests to finish, at most timeout milliseconds
        // TODO bufHandler.close(timeout);
        closeConnections();
    }


    @Override
    protected long getTimeout() {
        return 1000;
    }

    public int getPort() {
        return this.serverChannel.socket().getLocalPort();
    }
}
