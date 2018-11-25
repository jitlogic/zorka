package com.jitlogic.netkit;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;

public class TlsContext {

    public static final int NONE      = 0;
    public static final int HANDSHAKE = 1;
    public static final int DATA      = 2;
    public static final int CLOSING   = 3;
    public static final int CLOSED    = 4;
    public static final int ERROR     = 5;

    private SSLEngine sslEngine;

    private int sslState = NONE;

    private ByteBuffer sslAppBuf;

    private ByteBuffer sslNetBuf;

    public SSLEngine getSslEngine() {
        return sslEngine;
    }

    public void setSslEngine(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }

    public int getSslState() {
        return sslState;
    }

    public void setSslState(int sslState) {
        this.sslState = sslState;
    }

    public ByteBuffer getSslAppBuf() {
        return sslAppBuf;
    }

    public void setSslAppBuf(ByteBuffer sslAppBuf) {
        this.sslAppBuf = sslAppBuf;
    }

    public ByteBuffer getSslNetBuf() {
        return sslNetBuf;
    }

    public void setSslNetBuf(ByteBuffer sslNetBuf) {
        this.sslNetBuf = sslNetBuf;
    }

}
