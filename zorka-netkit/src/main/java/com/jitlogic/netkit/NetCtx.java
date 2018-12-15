/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.netkit;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class NetCtx {

    public static final String UNKNOWN_HOST = "127.0.0.1";

    /** Empty byte buffer, useful in some situations */
    public static final ByteBuffer NULL = ByteBuffer.wrap(new byte[0]);

    /** Sentinel object that will close connection. */
    public static final ByteBuffer CLOSE = ByteBuffer.wrap(new byte[0]);

    /** Sentinel object that will force connection flush. */
    public static final ByteBuffer FLUSH = ByteBuffer.wrap(new byte[0]);

    private LinkedList<ByteBuffer> writes = null;

    private SocketChannel channel;

    private BufHandler input;
    private BufHandler output;

    private TlsContext tlsContext;


    public static NetCtx fromKey(SelectionKey key) {
        return (NetCtx) key.attachment();
    }

    public NetCtx(SocketChannel channel, BufHandler input, BufHandler output) {
        this.channel = channel;
        this.input = input;
        this.output = output;
    }


    public synchronized void addWrite(ByteBuffer buf) {
        if (writes == null) {
            writes = new LinkedList<ByteBuffer>();
        }
        this.writes.add(buf);
    }


    public synchronized void addWrites(Collection<ByteBuffer> bufs) {
        if (writes == null) {
            writes = new LinkedList<ByteBuffer>();
        }
        this.writes.addAll(bufs);
    }


    public synchronized boolean hasWrites() {
        return writes != null && !writes.isEmpty();
    }


    public synchronized ByteBuffer[] peekWrites() {

        if (writes == null) {
            return new ByteBuffer[0];
        }

        if (writes.peekFirst() == CLOSE) {
            return new ByteBuffer[] { writes.peekFirst() };
        }

        // TODO filter out NULLs here

        ByteBuffer[] rslt = new ByteBuffer[writes.size()];
        writes.toArray(rslt);
        if (rslt.length > 1) {
            for (int i = 1; i < rslt.length; i++) {
                if (rslt[i] == CLOSE) {
                    return Arrays.copyOf(rslt, i);
                }
            }
        }
        return rslt;
    }


    public synchronized ByteBuffer peekWrite() {
        return writes != null ? writes.peekFirst() : null;
    }


    public synchronized boolean cleanWrites() {
        if (writes != null) {
            if (writes.peekFirst() == CLOSE) {
                writes.removeFirst();
            } else {
                while (writes.peekFirst() != CLOSE && writes.peekFirst() != null && !writes.peekFirst().hasRemaining()) {
                    writes.removeFirst();
                }
            }
        }
        return writes == null || writes.isEmpty();
    }


    public BufHandler getInput() {
        return input;
    }


    public BufHandler getOutput() {
        return output;
    }


    public SocketChannel getChannel() {
        return channel;
    }


    public String getServerName() {
//        try {
//            return channel != null ? ((InetSocketAddress)channel.getLocalAddress()).getHostName() : UNKNOWN_HOST;
//        } catch (IOException e) {
//            return UNKNOWN_HOST;
//        }
        return "localhost";
    }


    public int getServerPort() {
//        try {
//            //return channel != null ? ((InetSocketAddress)channel.getLocalAddress()).getPort() : -1;
//            return 80; // TODO
//        } catch (IOException e) {
//            return -1;
//        }
        return 80;
    }


    public String getRemoteAddr() {
        return UNKNOWN_HOST;
    }


    public TlsContext getTlsContext() {
        return tlsContext;
    }


    public void setTlsContext(TlsContext getTlsContext) {
        this.tlsContext = getTlsContext;
    }
}
