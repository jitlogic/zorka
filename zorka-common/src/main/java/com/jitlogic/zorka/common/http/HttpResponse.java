package com.jitlogic.zorka.common.http;

public class HttpResponse extends HttpMsg  {

    private int status;
    private String statusMsg;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }
}
