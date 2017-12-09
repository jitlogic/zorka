package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class TcpTextOutput extends ZorkaAsyncThread<String> {

    private static Logger log = LoggerFactory.getLogger(TcpTextOutput.class);

    private String host = "127.0.0.1";
    private int port = 9999;
    private int timeout = 30000;

    public TcpTextOutput(String name, Map<String,String> config) {
        super(name);

        // TODO parse addr = host:port if present
        if (config.get("addr") != null) {
            String addr = config.get("addr").trim();
            if (addr.contains(":")) {
                String[] s = addr.split(":");  // TODO use regex
                this.host = s[0];
                this.port = Integer.parseInt(s[1]);  // TODO proper host:port parsing function, common for all
            } else {
                this.host = addr;
            }
        } else {
            if (config.get("host") != null) {
                this.host = config.get("host");
            }

            String pstr = config.get("port");
            if (pstr != null) {
                pstr = pstr.trim();
                if (pstr.matches("\\d+")) {
                    this.port = Integer.parseInt(pstr);
                } else {
                    log.error("Cannot parse TCP port number: '" + pstr + "'");
                }
            }

        }

        String ptmo = config.get("timeout");
        if (ptmo != null) {
            ptmo = ptmo.trim();
            if (ptmo.matches("\\d+")) {
                this.timeout = Integer.parseInt(ptmo);
            } else {
                log.error("Cannot parse TCP timeout: '" + ptmo + "'");
            }
        }
    }

    @Override
    protected void process(List<String> msgs) {
        log.debug("Received " + msgs.size() + " messages to send.");
        for (String msg : msgs) {
            if (log.isTraceEnabled()) {
                log.trace("Sending data to " + host + ":" + port + ": " + msg);
            }
            Socket socket = null;
            try {
                socket = new Socket(host, port);
                socket.setSoTimeout(timeout);
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
                // TODO AgentDiagnostics.inc(true, AgentDiagnostics.COUNTER_ID);
            } catch (IOException e) {
                log.error("Error sending data to " + host + ":" + port, e);
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    log.error("Error closing socket to " + host + ":" + port, e);
                }
            }
        }
    }
}
