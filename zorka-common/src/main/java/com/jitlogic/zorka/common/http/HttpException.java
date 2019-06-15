package com.jitlogic.zorka.common.http;

public class HttpException extends RuntimeException {

    private final int status;

    private final String statusLine;

    private final Object data;

    public HttpException(String msg) {
        this(msg, null);
    }

    public HttpException(String msg, Throwable e) {
        super(msg, e);
        this.status = 0;
        this.statusLine = null;
        this.data = null;
    }

    public HttpException(String msg, int status, String statusLine, Object data, Throwable e) {
        super(msg, e);
        this.status = status;
        this.statusLine = statusLine;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public Object getData() {
        return data;
    }
}
