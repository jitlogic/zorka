package com.jitlogic.zorka.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpServerConnection implements Runnable {

    private Logger log = LoggerFactory.getLogger(HttpServerConnection.class);

    private Socket socket;
    private HttpHandler listener;
    private HttpConfig config;

    public HttpServerConnection(HttpConfig config, Socket socket, HttpHandler listener) {
        this.socket = socket;
        this.listener = listener;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            HttpDecoder d = new HttpDecoder(is, config);
            OutputStream os = socket.getOutputStream();
            HttpEncoder e = new HttpEncoder(config, "", os);

            for (HttpMessage mi = d.decode(false); mi != null; mi = d.decode(false)) {
                HttpMessage mo = listener.handle(mi);
                e.handle(mo);
                os.flush();
            }
        } catch (IOException e) {
            log.error("I/O error", e);
        } catch (HttpClosedException e) {
            // do nothing
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("I/O error", e);
            }
        }
    }
}
