package com.jitlogic.netkit;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;

public abstract class NetRequest implements Comparable<NetRequest>, BufHandler {

    private SocketAddress remoteAddress;
    private long timeout;
    protected boolean freshConnect = true;

    public NetRequest(String addr, int port) {
        this.remoteAddress = new InetSocketAddress(addr, port);
    }

    public abstract void sendRequest(SelectionKey key, BufHandler output); // TODO get rid of output argument

    public long getTimeout() {
        return timeout;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public int compareTo(NetRequest r) {
        return (int)(timeout - r.timeout);
    }

}
