package com.jitlogic.zorka.net.http.nano;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.net.TcpSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class HttpConnectionHandlerFactory implements TcpSessionFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandlerFactory.class);

    private IHandler<IHTTPSession, Response> handler;

    public HttpConnectionHandlerFactory(IHandler<IHTTPSession, Response> handler) {
        this.handler = handler;
    }

    private void handle(Socket socket) {
        OutputStream os = null;
        InputStream is = null;

        try {
            // TODO set socket timeout eventually here
            is = socket.getInputStream();
            os = socket.getOutputStream();
            HTTPSession session = new HTTPSession(handler, is, os, socket.getInetAddress());
            while (!socket.isClosed()) {
                session.execute();
            }
        } catch (SocketException e) {
            if (!"NanoHttpd Shutdown".equals(e.getMessage())) {
                log.warn("Handling HTTP connection", e);
            }
        } catch (Exception e) {
            log.error("Error handling HTTP connection.", e);
        } finally {
            ZorkaUtil.close(is);
            ZorkaUtil.close(os);
            ZorkaUtil.close(socket);
        }
    }

    @Override
    public Runnable getSession(final Socket socket) {
        return new Runnable() {
            @Override
            public void run() {
                handle(socket);
            }
        };
    }
}
