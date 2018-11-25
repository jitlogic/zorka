package com.jitlogic.netkit;


import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class NetClient extends NetEngine implements NetRequestListener {

    // TODO move timeouts to server side, implement common client and server timeout implementation;
    private PriorityQueue<NetRequest> runRequests = new PriorityQueue<NetRequest>();

    private ConcurrentLinkedQueue<NetRequest> newQueue = new ConcurrentLinkedQueue<NetRequest>();

    public NetClient(String threadName, SSLContext context) throws IOException {
        this(threadName, NetCtxFactory.DEFAULT, context);
    }

    public NetClient(String threadName, NetCtxFactory ctxFactory, SSLContext context) throws IOException {
        super(threadName, ctxFactory, context);
    }

    @Override
    public void start() {
        running = true;
        thread = new Thread(this, "client-thread");
        thread.start();
    }

    @Override
    protected long getTimeout() {
        return 1000;
    }

    @Override
    public synchronized void stop(int timeout) {
        // TODO handle timeout and running connections
        running = false;
    }

    @Override
    public boolean submit(NetRequest request) {
        if (running) {
            boolean rslt = newQueue.offer(request);
            if (rslt) selector.wakeup();
            return rslt;
        } else {
            return false;
        }
    }

    protected void connect(SelectionKey key) {
        NetCtx atta = NetCtx.fromKey(key);
        NetRequest req = (NetRequest)atta.getInput();

        try {
            boolean connected = atta.getChannel().finishConnect();
            if (connected) {
                // TODO
                key.interestOps(OP_WRITE|OP_READ);
                // TODO if TLS enabled, begin handshake here ...
                req.sendRequest(key, this);
            }
        } catch (IOException e) {
            // TODO notify client that connection failed, or retry
            log.error("Cannot finish connect for " + req, e);
        }

    }

    protected void processNew() {
        for (NetRequest req = newQueue.poll(); req != null; req = newQueue.poll()) {
            SocketChannel ch = null;
            try {
                ch = SocketChannel.open();
                // TODO implement for JDK6
                //ch.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
                //ch.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
                ch.configureBlocking(false);
                boolean connected = ch.connect(req.getRemoteAddress());
                NetCtx atta = ctxFactory.create(ch, req, this);
                ch.register(selector, connected ? OP_READ|OP_WRITE : SelectionKey.OP_CONNECT, atta);
                // TODO limit OP_WRITE|OP_READ, can start sending here if connected = true
                selector.wakeup();
                //req.setSelectionKey(key);
                ch = null;
            } catch (Exception e) {
                log.error("Cannot process client request " + req, e);
                newQueue.offer(req);
            } finally {
                if (ch != null) {
                    try {
                        ch.close();
                    } catch (IOException e) {
                        log.error("Cannot close channel " + ch, e);
                    }
                }
            }
        }
    }
}
