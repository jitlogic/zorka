package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.NetCtx;
import com.jitlogic.netkit.NetRequest;
import com.jitlogic.netkit.NetRequestListener;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static org.junit.Assert.*;

/**
 * Simulates client-server network connection.
 */
public class TestNetExchange implements BufHandler, NetRequestListener {

    private SelectionKey serverKey, clientKey;
    private BufHandler server, client;

    public TestNetExchange(BufHandler client, BufHandler server) {
        this.client = client;
        this.server = server;
    }

    private void initKeys() {
        assertNotNull("server not set", server);
        assertNotNull("client not set", client);
        if (clientKey == null) {
            clientKey = new TestSelectionKey("client");
            clientKey.attach(new NetCtx(null, client, this));
        }
        if (serverKey == null) {
            serverKey = new TestSelectionKey("server");
            serverKey.attach(new NetCtx(null, server, this));
        }
    }

    public SelectionKey clientKey() {
        initKeys();
        return clientKey;
    }

    public SelectionKey serverKey() {
        initKeys();
        return serverKey;
    }

    @Override
    public boolean submit(SelectionKey key, boolean copyOnSchedule, ByteBuffer... buffers) {
        initKeys();
        if (key == serverKey) {
            return client.submit(clientKey, copyOnSchedule, buffers);
        } else {
            return server.submit(serverKey, copyOnSchedule, buffers);
        }
    }

    @Override
    public boolean submit(NetRequest req) {
        req.sendRequest(this.clientKey(), this);
        return true;
    }
}
