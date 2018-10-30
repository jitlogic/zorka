/*
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.net.http.nano;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.net.TcpSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class HttpConnectionHandler implements TcpSessionFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandler.class);

    private IHandler<IHTTPSession, Response> handler;

    public HttpConnectionHandler(IHandler<IHTTPSession, Response> handler) {
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
