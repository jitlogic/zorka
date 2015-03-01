/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

/**
 * Client connector for ZICO protocol.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZicoClientConnector extends ZicoConnector {

    private int socketTimeout;

    /**
     * Creates client connector with default socket timeout of 30 seconds.
     *
     * @param addr hostname or IP address of ZICO server
     * @param port ZICO server port number
     * @throws IOException if connection or server name resolution fails
     */
    public ZicoClientConnector(String addr, int port) throws IOException {
        this(addr, port, 30000);
    }

    /**
     * Creates client connector with configurable socket timeout
     *
     * @param addr          hostname or IP address of ZICO server
     * @param port          ZICO server port number
     * @param socketTimeout socket timeout (in milliseconds)
     * @throws IOException if connection of server name resolution fails
     */
    public ZicoClientConnector(String addr, int port, int socketTimeout) throws IOException {
        this.addr = InetAddress.getByName(addr);
        this.port = port;
        this.socketTimeout = socketTimeout;
    }


    /**
     * (Re)connects to ZICO server
     *
     * @throws IOException if connection fails
     */
    public void connect() throws IOException {
        socket = new Socket(addr, port);
        socket.setSoTimeout(socketTimeout);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        // TODO log connection here
    }


    /**
     * Sends ZICO PING packet and awaits reply.
     *
     * @return ZICO ping roundtrip time
     * @throws IOException if connection fails or reply packet received by server is invalid
     */
    public long ping() throws IOException {
        long t1 = System.nanoTime();
        send(ZICO_PING);
        if (recv().getStatus() != ZICO_PONG) {
            throw new ZicoException(ZICO_BAD_REPLY, "Expected PONG reply.");
        }
        long t2 = System.nanoTime();
        return t2 - t1;
    }


    /**
     * Sends HELLO packet and awaits reply. This is equivalent of 'log in' to ZICO server.
     * No communication or data submission (except for PING) is possible prior to this operation.
     *
     * @param hostname client name (as advertised to collector server), will appear in host list;
     * @param auth     client pass phrase (only if server is working in secure mode);
     * @throws IOException if connection breaks, client authentication error or other error occurs
     */
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


    /**
     * Submits data to collector server. Data object will be encoded into fressian format and sent.
     *
     * @param data object to be submitted
     * @throws IOException if connection breaks or server error occurs when processing submitted data;
     */
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
