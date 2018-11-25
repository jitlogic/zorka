package com.jitlogic.netkit.http;

public class HttpConfig {

    public static final int KEEP_ALIVE = 30;
    public static final int MAX_LINE_SIZE = 32 * 1024;
    public static final int MAX_BODY_SIZE = 8 * 1024 * 1024;

    private int keepAliveTimeout = KEEP_ALIVE;
    private int maxLineSize = MAX_LINE_SIZE;
    private int maxBodySize = MAX_BODY_SIZE;

    private String keepAliveString = "timeout=" + keepAliveTimeout;

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
}
