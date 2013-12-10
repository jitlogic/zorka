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
package com.jitlogic.zico.core;


import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.zico.ZicoCommonUtil;
import com.jitlogic.zorka.common.zico.ZicoConnector;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;

import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_AUTH_ERROR;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_BAD_REQUEST;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_DATA;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_HELLO;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_OK;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_PING;
import static com.jitlogic.zorka.common.zico.ZicoPacket.ZICO_PONG;

public class ZicoServerConnector extends ZicoConnector implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ZicoServerConnector.class);

    private ZicoDataProcessorFactory factory;
    private ZicoDataProcessor context = null;

    private SocketAddress saddr;

    public ZicoServerConnector(Socket socket, ZicoDataProcessorFactory factory) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.factory = factory;

        this.saddr = socket.getRemoteSocketAddress();

        // TODO this is a crutch; socket timeout or config object should be supplied by injector;
        if (factory instanceof HostStoreManager) {
            this.socket.setSoTimeout(((HostStoreManager) factory).getConfig().intCfg("zico.socket.timeout", 1200000));
        }
    }

    private volatile boolean running = true;


    private void runCycle() throws IOException {
        ZicoPacket pkt = recv();
        switch (pkt.getStatus()) {
            case ZICO_PING: {
                send(ZICO_PONG);
                break;
            }
            case ZICO_HELLO: {
                List<Object> lst = ZicoCommonUtil.unpack(pkt.getData());
                log.debug("Encountered ZICO HELLO packet: " + lst + "(addr=" + saddr + ")");
                if (lst.size() > 0 && lst.get(0) instanceof HelloRequest) {
                    context = factory.get(socket, (HelloRequest) lst.get(0));
                    send(ZICO_OK);
                } else {
                    log.error("ZICO_HELLO packet with invalid content: " + lst + "(addr=" + addr + ")");
                    send(ZICO_BAD_REQUEST);
                }
                break;
            }
            case ZICO_DATA: {
                log.debug("Received ZICO data packet from " + saddr + ": status=" + pkt.getStatus()
                        + ", dlen=" + pkt.getData().length);
                if (context != null) {
                    for (Object o : ZicoCommonUtil.unpack(pkt.getData())) {
                        context.process(o);
                    }
                    send(ZICO_OK);
                } else {
                    log.error("Client " + saddr + " not authorized.");
                    send(ZICO_AUTH_ERROR);
                }
                break;
            }
            default:
                log.error("ZICO packet from " + saddr + " with invalid status code: " + pkt.getStatus());
                send(ZICO_BAD_REQUEST);
                break;
        }
    }


    @Override
    public void run() {
        try {
            while (running) {
                runCycle();
            }
        } catch (SocketTimeoutException e) {
            log.info("Client connection " + saddr + " is inactive. Closing.");
        } catch (EOFException e) {
            log.info("Client " + saddr + " disconnected.");
        } catch (ZicoException ze) {
            log.error("Got ZICO exception (addr=" + saddr + ", status=" + ze.getStatus() + ")", ze);
            try {
                send(ze.getStatus());
            } catch (IOException e) {
            }
        } catch (Exception e) {
            log.error("Error in client " + saddr + " connection main loop.", e);
            running = false;
        } finally {
            try {
                close();
            } catch (IOException e) {
            }
        }
    }


    @Override
    public void close() throws IOException {
        super.close();
        running = false;
    }
}
