/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.zico;

import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import org.fressian.FressianWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static com.jitlogic.zorka.common.zico.ZicoPacket.*;

public class ZicoClientConnector extends ZicoConnector {

    private int timeout;

    public ZicoClientConnector(String addr, int port) throws IOException {
        this(addr, port, 30000);
    }

    public ZicoClientConnector(String addr, int port, int timeout) throws IOException {
        this.addr = InetAddress.getByName(addr);
        this.port = port;
        this.timeout = timeout;
    }


    public void connect() throws IOException {
        socket = new Socket(addr, port);
        socket.setSoTimeout(timeout);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        // TODO log connection here
    }


    public long ping() throws IOException {
        long t1 = System.nanoTime();
        send(ZICO_PING);
        if (recv().getStatus() != ZICO_PONG) {
            throw new ZicoException(ZICO_BAD_REPLY, "Expected PONG reply.");
        }
        long t2 = System.nanoTime();
        return t2 - t1;
    }


    public void hello(String hostname, String auth) throws IOException {
        send(ZICO_HELLO, ZicoCommonUtil.pack(
                new HelloRequest(System.currentTimeMillis(), hostname, auth)));
        ZicoPacket pkt = recv();
        switch (pkt.getStatus()) {
            case ZICO_OK:
                return;
            case ZICO_AUTH_ERROR:
                throw new ZicoException(ZICO_AUTH_ERROR, "Authentication error.");
            default:
                throw new ZicoException(pkt.getStatus(), "Other error: status=" + pkt.getStatus());
        }
    }


    public void submit(Object data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(data);
        writer.close();
        send(ZICO_DATA, ZicoCommonUtil.pack(data));
        ZicoPacket pkt = recv();
        if (pkt.getStatus() != ZICO_OK) {
            throw new ZicoException(pkt.getStatus(), "ZICO submission error: status=" + pkt.getStatus());
        }
    }

}
