package com.jitlogic.netkit.http;

import com.jitlogic.netkit.NetException;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class HttpConfig {

    private static final int MAX_RETRIES = 3;
    public static final int KEEP_ALIVE = 30;
    public static final int MAX_LINE_SIZE = 32 * 1024;
    public static final int MAX_BODY_SIZE = 8 * 1024 * 1024;

    private int keepAliveTimeout = KEEP_ALIVE;
    private int maxLineSize = MAX_LINE_SIZE;
    private int maxBodySize = MAX_BODY_SIZE;
    private int maxRetries = MAX_RETRIES;

    private SSLContext sslContext;

    private String keepAliveString = "timeout=" + keepAliveTimeout;

    public SSLContext getSslContext() {
        try {
            return sslContext != null ? sslContext : SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new NetException("Error initializing SSL context", e);
        }
    }

    public void setSslContext(SSLContext context) {
        this.sslContext = context;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getMaxLineSize() {
        return maxLineSize;
    }

    public void setMaxLineSize(int maxLineSize) {
        this.maxLineSize = maxLineSize;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public String getKeepAliveString() {
        return keepAliveString;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
