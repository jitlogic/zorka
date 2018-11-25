package com.jitlogic.netkit;

import com.jitlogic.netkit.log.EventSink;
import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.jitlogic.netkit.TlsContext.*;


public abstract class NetEngine implements Runnable, BufHandler {

    protected Logger log  = LoggerFactory.getLogger(this.getClass());

    // queue operations from worker threads to the IO thread
    protected final ConcurrentLinkedQueue<PendingKey> pending = new ConcurrentLinkedQueue<PendingKey>();

    protected String threadName;

    protected Selector selector;
    protected NetCtxFactory ctxFactory;
    protected SSLContext sslContext;

    protected EventSink evtLoops = LoggerFactory.getSink(LoggerFactory.EV_LOOPS);
    protected EventSink evtIllegalOps = LoggerFactory.getSink(LoggerFactory.EV_ILLEGAL_OPS);

    protected Executor sslExecutor;

    protected Thread thread;
    protected volatile boolean running;

    // shared, single thread
    protected ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 64 - 1);

    public NetEngine(String threadName, NetCtxFactory ctxFactory, SSLContext context) throws IOException {
        this.sslContext = context;
        this.threadName = threadName;
        this.selector = Selector.open();
        this.ctxFactory = ctxFactory;

        if (sslContext != null) {
            sslExecutor = Executors.newSingleThreadExecutor();
        } else {
            sslExecutor = null;
        }

    }

    public abstract void stop(int timeout);

    protected NetCtx atta(SelectionKey key) {
        return (NetCtx) key.attachment();
    }

    private void doRead(final SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        BufHandler bufHandler = atta(key).getInput();
        try {
            buffer.clear(); // clear for read
            int read = ch.read(buffer);
            if (read == -1) {
                // remote entity shut the socket down cleanly.
                clientClose(key);
            } else if (read > 0) {
                buffer.flip(); // flip for read
                bufHandler.submit(key, false, buffer);
            }
        } catch (IOException e) { // the remote forcibly closed the connection
            clientClose(key);
        }
    }

    private void doWrite(SelectionKey key) {
        NetCtx atta = (NetCtx) key.attachment();
        SocketChannel ch = (SocketChannel) key.channel();
        try {
            // the sync is per socket (per client). virtually, no contention
            // 1. keep byte data order, 2. ensure visibility
            synchronized (atta) {
                if (atta.peekWrite() == NetCtx.CLOSE) {
                    clientClose(key);
                } else if (atta.hasWrites()) {
                    ByteBuffer[] bufs = atta.peekWrites();
                    ch.write(bufs, 0, bufs.length);
                    // TODO handle CLOSE marks here ...
                    if (atta.cleanWrites()) {
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
        } catch (IOException e) { // the remote forcibly closed the connection
            clientClose(key);
        }
    }

    private void sslDataRead(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        NetCtx atta = (NetCtx)key.attachment();

        log.traceNio(key, "sslDataRead()", "ENTER");

        TlsContext tctx = atta.getTlsContext();

        if (tctx.getSslNetBuf() == null) {
            tctx.setSslNetBuf(ByteBuffer.allocate(tctx.getSslEngine().getSession().getPacketBufferSize()));
            tctx.getSslNetBuf().clear();
        }

        try {
            tctx.getSslNetBuf().clear(); // clear for read
            int read = ch.read(tctx.getSslNetBuf());
            if (read > 0) {
                tctx.getSslNetBuf().flip();
                while (tctx.getSslNetBuf().hasRemaining()) {
                    buffer.clear();
                    log.traceNio(key, "sslDataRead()->unwrap()", "BEFORE");
                    SSLEngineResult result = tctx.getSslEngine().unwrap(tctx.getSslNetBuf(), buffer);
                    log.traceNio(key, "sslDataRead()->unwrap()", result.getStatus().toString());
                    switch (result.getStatus()) {
                        case OK:
                            buffer.flip();
                            atta(key).getInput().submit(key, false, buffer);
                            break;
                        case BUFFER_OVERFLOW:
                            log.error("at sslDataRead: BUFFER_OVERFLOW should not happen");
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            return;
                        case BUFFER_UNDERFLOW:
                            tctx.setSslNetBuf(extendBuf(tctx.getSslNetBuf(), tctx.getSslEngine().getSession().getPacketBufferSize(), 1024, true));
                            return;
                        case CLOSED:
                            clientClose(key);
                            tctx.setSslState(CLOSED);
                            break;
                        default:
                            log.error("illegal state: " + result.getStatus());
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            return;
                    } // switch
                } // while
                tctx.getSslNetBuf().clear();
            } else {
                // TODO handleEndOfStream() -> send CLOSE message (if possible)
                if (log.isTraceEnabled())
                    log.trace("sslDataRead(): end-of-stream key=" + key);

                clientClose(key);
            }
        } catch (IOException e) { // the remote forcibly closed the connection
            log.error("at sslDataRead(): key=" + key + ", sslState=" + tctx.getSslState(), e);
            clientClose(key);
        }

    }


    private void sslHandshake(final SelectionKey key) {
        NetCtx atta = atta(key);
        SocketChannel ch = (SocketChannel) key.channel();
        TlsContext tctx = atta.getTlsContext();
        SSLEngine engine = tctx.getSslEngine();
        SSLEngineResult.HandshakeStatus handshakeStatus;
        SSLEngineResult result;

        log.traceNio(key, "sslHandshake()", "ENTER");

        if (tctx.getSslAppBuf() == null) {
            tctx.setSslAppBuf(ByteBuffer.allocate(engine.getSession().getApplicationBufferSize()));
            tctx.getSslAppBuf().clear();
        }

        if (tctx.getSslNetBuf() == null) {
            tctx.setSslNetBuf(ByteBuffer.allocate(engine.getSession().getPacketBufferSize()));
            tctx.getSslNetBuf().clear();
        }

        handshakeStatus = engine.getHandshakeStatus();

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
                handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            // TODO this loop should break when handshake operation is in progress
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    try {
                        int rb = ch.read(tctx.getSslNetBuf());
                        if (rb < 0) {
                            forceClose(key, atta, tctx, engine);
                            return;
                        }
                        tctx.getSslNetBuf().flip();
                        try {
                            log.traceNio(key, "sslHandshake()->unwrap()", "BEFORE");
                            result = engine.unwrap(tctx.getSslNetBuf(), tctx.getSslAppBuf());
                            log.traceNio(key, "sslHandshake()->unwrap()", "AFTER");
                            tctx.getSslNetBuf().compact();
                            handshakeStatus = result.getHandshakeStatus();
                        } catch (SSLException e) {
                            log.error("at sslHandshake / unwrap", e);
                            engine.closeOutbound();
                            handshakeStatus = engine.getHandshakeStatus();
                            break;
                        }
                    } catch (IOException e) {
                        log.error("at sslHandshake", e);
                        clientClose(key);
                        tctx.setSslState(ERROR);
                        log.traceNio(key, "sslHandshake()->unwrap()", "ERROR");
                        return;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_OVERFLOW:
                            tctx.setSslAppBuf(extendBuf(tctx.getSslAppBuf(), engine.getSession().getApplicationBufferSize(), 0, false));
                            log.traceNio(key, "sslHandshake()->unwrap()", "OVERFLOW");
                            break;
                        case BUFFER_UNDERFLOW:
                            tctx.setSslNetBuf(extendBuf(tctx.getSslNetBuf(), engine.getSession().getPacketBufferSize(), 0, true));
                            log.traceNio(key, "sslHandshake->unwrap()", "UNDERFLOW");
                            return;
                        case CLOSED: {
                            if (engine.isOutboundDone()) {
                                log.error("SSL engine unexpectedly closed.");
                                clientClose(key);
                                tctx.setSslState(ERROR);
                                return;
                            } else {
                                engine.closeOutbound();
                                handshakeStatus = engine.getHandshakeStatus();
                            }
                            break;
                        }
                        default:
                            log.error("sslHandshake: illegal state");
                            clientClose(key);
                    } // switch
                    break;
                case NEED_WRAP:
                    tctx.getSslNetBuf().clear();
                    try {
                        log.traceNio(key, "sslHandshake()->wrap()", "BEFORE");
                        result = engine.wrap(tctx.getSslAppBuf(), tctx.getSslNetBuf());
                        log.traceNio(key, "sslHandshake()->wrap()", "AFTER");
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (IOException e) {
                        log.error("at sslHandshake, wrap() failed", e);
                        engine.closeOutbound();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK:
                            tctx.getSslNetBuf().flip();
                            if (!atta.getOutput().submit(key, true, tctx.getSslNetBuf())) {
                                return;
                            }
                            tctx.getSslNetBuf().clear();
                            break;
                        case BUFFER_OVERFLOW:
                            tctx.setSslNetBuf(extendBuf(tctx.getSslNetBuf(), tctx.getSslEngine().getSession().getPacketBufferSize(), 0, true));
                            break;
                        case BUFFER_UNDERFLOW:
                            log.error("at sslHandshake, BUFFER_UNDERFLOW or BUFFER_OVERFLOW should not happen at write");
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            return;
                        case CLOSED:
                            try {
                                tctx.getSslNetBuf().flip();
                                while (tctx.getSslNetBuf().hasRemaining()) {
                                    ch.write(tctx.getSslNetBuf());
                                }
                                tctx.getSslNetBuf().clear();
                            } catch (IOException e) {
                                log.error("sslHandshake(): key=" + key + ": failed to send CLOSE message to client");
                                handshakeStatus = engine.getHandshakeStatus();
                            }
                            break;
                        default:
                            log.error("Invalid SSL status: " + result.getStatus());
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            return;
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        log.traceNio(key, "sslHandshake()->runTask()", "BEGIN");
                        final Runnable t = task;
                        sslExecutor.execute(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        t.run();
                                        pending.add(new PendingKey(key, PendingKey.OP_HANDSHAKE));
                                        selector.wakeup();
                                    }
                                }
                        );
                    }
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                default:
                    clientClose(key);
                    log.error("Error terror");
                    tctx.setSslState(ERROR);
                    break;
            } // switch
        } // while

        tctx.setSslState(DATA);
    }


    protected void clientClose(final SelectionKey key) {
        log.traceNio(key,"clientClose()", "ENTER");
        try {
            key.channel().close();
        } catch (Exception ignore) {
        }

        NetCtx att = (NetCtx) key.attachment();

        if (att != null) {
            synchronized (att) {
                if (att.getTlsContext() != null)
                    att.getTlsContext().setSslState(CLOSING);
            }
        }
    }


    // TODO this should be removed and merged into clientClose(); (and clientClose should be fixed)
    private void forceClose(SelectionKey key, NetCtx atta, TlsContext tctx, SSLEngine engine) {
        try {
            if (!engine.isInboundDone()) {
                engine.closeInbound();
            }
            if (!engine.isOutboundDone()) {
                engine.closeOutbound();
            }
            atta.getInput().submit(key, false, NetCtx.CLOSE);
        } catch (Exception e) {
            log.debug("forcible close: " + e.getMessage());
        }
        try {
            key.interestOps(0);
            key.channel().close();
        } catch (Exception e) {
            log.debug("forcibly closing SSL connection: " + e.getMessage());
        }

        tctx.setSslState(CLOSED);
        return;
    }

    private int remaining(ByteBuffer...ibufs) {
        int rslt = 0;
        for (int i = 0; i < ibufs.length; i++) {
            rslt += ibufs[i].remaining();
        }
        return rslt;
    }

    private LinkedList<ByteBuffer> sslEncode(SelectionKey key, ByteBuffer...ibufs) {
        NetCtx atta = (NetCtx)key.attachment();
        TlsContext tctx = atta.getTlsContext();
        LinkedList<ByteBuffer> obufs = new LinkedList<ByteBuffer>();
        try {
            synchronized (atta) {
                while (remaining(ibufs) > 0) {
                    ByteBuffer sslWrite = ByteBuffer.allocate(16384 + 4096);
                    sslWrite.clear();
                    SSLEngineResult result = tctx.getSslEngine().wrap(ibufs, sslWrite);

                    switch (result.getStatus()) {
                        case OK:
                            sslWrite.flip();
                            obufs.add(sslWrite);
                            break;
                        case BUFFER_OVERFLOW:
                            log.error("at sslEncode(), BUFFER_OVERFLOW should not happen");
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            break;
                        case BUFFER_UNDERFLOW:
                            log.error("at sslEncode(), BUFFER_UNDERFLOW should not happen");
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            break;
                        case CLOSED:
                            clientClose(key);
                            tctx.setSslState(CLOSED);
                            break;
                        default:
                            log.error("at sslEncode(), illegal status:" + result.getStatus());
                            clientClose(key);
                            tctx.setSslState(ERROR);
                            break;
                    }
                } // while
            }
        } catch (IOException e) {
            log.error("at sslEncode()", e);
            clientClose(key);
            tctx.setSslState(ERROR);
        }
        return obufs;
    }

    @Override
    public boolean submit(final SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        NetCtx atta = (NetCtx) key.attachment();
        TlsContext tctx = atta.getTlsContext();
        ByteBuffer[] bufs = buffers;

        synchronized (atta) {

            if (tctx != null && tctx.getSslState() == DATA) {
                LinkedList<ByteBuffer> sbufs = sslEncode(key, bufs);
                atta.addWrites(sbufs);
                // TODO this breaks single-write optimization in SSL mode
                bufs = sbufs.toArray(new ByteBuffer[0]);
            }

            boolean hasClose = false;

            for (int i = 0; i < buffers.length; i++) {
                // TODO if no writes and there is something before CLOSE mark, write everything up to
                if (buffers[i] == NetCtx.CLOSE) hasClose = true;
            }

            if (!atta.hasWrites() && !hasClose) {
                SocketChannel ch = (SocketChannel) key.channel();
                try {
                    // TCP buffer most of time is empty, writable(8K ~ 256k)
                    // One IO thread => One thread reading + Many thread writing
                    // Save 2 system call
                    ch.write(bufs, 0, bufs.length);
                    if (bufs[bufs.length - 1].hasRemaining()) {
                        scheduleWrites(copyOnSchedule, atta, bufs);
                        pending.add(new PendingKey(key, PendingKey.OP_WRITE));
                        selector.wakeup();
                        return false;
                    }
                } catch (IOException e) {
                    pending.add(new PendingKey(key, PendingKey.OP_WRITE));
                    selector.wakeup();
                    return false;
                }
            } else {
                // If has pending write(s), order should be maintained. (WebSocket)
                scheduleWrites(copyOnSchedule, atta, bufs);
                pending.add(new PendingKey(key, PendingKey.OP_WRITE));
                selector.wakeup();
                return false;
            }
        }
        return true;
    }

    private void scheduleWrites(boolean copyOnSchedule, NetCtx atta, ByteBuffer[] bufs) {
        // TODO when copyOnSchedule is true, coalesce everything into single buffer
        for (ByteBuffer b : bufs) {
            if (b.hasRemaining()) {
                if (copyOnSchedule) {
                    ByteBuffer b1 = ByteBuffer.allocate(b.remaining());
                    b1.clear();
                    b1.put(b);
                    b1.flip();
                    atta.addWrite(b1);
                } else {
                    atta.addWrite(b);
                }
            } else if (b == NetCtx.CLOSE) {
                atta.addWrite(b);
            }
        }
    }



    // TODO move this to utility class
    public ByteBuffer extendBuf(ByteBuffer orig, int minimumSize, int delta, boolean copy) {
        ByteBuffer bb = (orig.capacity() < minimumSize) ? ByteBuffer.allocate(minimumSize) :
                (delta > 0 && orig.capacity() == minimumSize) ? ByteBuffer.allocate(orig.capacity() + delta) : orig;
        if (copy && bb != orig) {
            orig.flip();
            bb.put(orig);
        }
        return bb;
    }

    public synchronized void start() {
        running = true;
        thread = new Thread(this, threadName);
        thread.start();
    }

    protected abstract long getTimeout();

    protected void accept(SelectionKey key) {
        evtIllegalOps.error();
    }

    protected void connect(SelectionKey key) {
        evtIllegalOps.error();
    }

    public void run() {
        while (running) {
            long t0 = evtLoops.time();
            try {
                processNew();
                processPending();

                if (selector.select(getTimeout()) <= 0) continue;

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (SelectionKey key : selectedKeys) {
                    log.traceNio(key, "run", "SELECTOR" + String.format("%02x",key.interestOps()));
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isReadable()) {
                        log.traceNio(key, "run", "SELECTOR/isReadable()");
                        NetCtx a = (NetCtx)key.attachment();
                        TlsContext tctx = a.getTlsContext();
                        if (tctx == null) {
                            doRead(key);
                        } else {
                            switch (tctx.getSslState()) {
                                case HANDSHAKE:
                                    sslHandshake(key);
                                    break;
                                case DATA:
                                    sslDataRead(key);
                                    break;
                                case CLOSING:
                                    sslHandshake(key);
                                    break;
                                default:
                                    clientClose(key);
                            }
                        }
                    } else if (key.isWritable()) {
                        log.traceNio(key, "run", "SELECTOR/isWritable()");
                        NetCtx a = (NetCtx)key.attachment();
                        TlsContext tctx = a.getTlsContext();
                        if (tctx == null) {
                            doWrite(key);
                        } else {
                            switch (tctx.getSslState()) {
                                case HANDSHAKE:
                                    sslHandshake(key);
                                    break;
                                case DATA:
                                    doWrite(key);
                                    break;
                                case CLOSING:
                                    sslHandshake(key);
                                    break;
                                default:
                                    clientClose(key);
                            }
                        }
                    }
                }
                selectedKeys.clear();
                evtLoops.call(t0);
            } catch (ClosedSelectorException ignore) {
                return; // stopped
                // do not exits the while IO event loop. if exits, then will not process any IO event
                // jvm can catch any exception, including OOM
            } catch (Throwable e) { // catch any exception(including OOM), print it
                log.error("http server loop error, should not happen", e);
                evtLoops.error(t0);
            }
        }
    }

    private void processPending() {
        PendingKey k;
        while (!pending.isEmpty()) {
            k = pending.poll();
            if (k.Op == PendingKey.OP_WRITE) {
                if (k.key.isValid()) {
                    k.key.interestOps(SelectionKey.OP_WRITE);
                }
            } else if (k.Op == PendingKey.OP_HANDSHAKE) {
                sslHandshake(k.key);
            } else {
                clientClose(k.key);
            }
        }
    }

    protected void processNew() {
    }

    protected void closeConnections() {
        if (selector.isOpen()) {
            boolean cmex;
            do {
                cmex = false;
                try{
                    for (SelectionKey k : selector.keys()) {
                        if (k != null) clientClose(k);
                    }
                } catch(java.util.ConcurrentModificationException ex) {
                    cmex = true;
                }
            } while(cmex);

            try {
                selector.close();
            } catch (IOException ignore) {
            }
        }
    }

}
