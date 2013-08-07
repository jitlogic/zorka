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


import com.jitlogic.zorka.common.tracedata.HelloRequest;
import static com.jitlogic.zorka.common.zico.ZicoPacket.*;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ZicoServerConnector extends ZicoConnector implements Runnable {

    private ZicoDataProcessorFactory factory;
    private ZicoDataProcessor context = null;

    public ZicoServerConnector(Socket socket, ZicoDataProcessorFactory factory) throws IOException {
        this.socket  = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.factory = factory;
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
                List<Object> lst = ZicoUtil.unpack(pkt.getData());
                if (lst.size() > 0 && lst.get(0) instanceof HelloRequest) {
                    context = factory.get(socket, (HelloRequest) lst.get(0));
                    send(ZICO_OK);
                } else {
                    // TODO log here
                    send(ZICO_BAD_REQUEST);
                }
                break;
            }
            case ZICO_DATA: {
                if (context != null) {
                    for (Object o : ZicoUtil.unpack(pkt.getData())) {
                        context.process(o);
                    }
                    send(ZICO_OK);
                } else {
                    send(ZICO_AUTH_ERROR);
                }
                break;
            }
            default:
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
        } catch (ZicoException ze) {
            // TODO log
            try { send(ze.getStatus()); } catch (IOException e) { }
        } catch (Exception e) {
            // TODO log
            running = false;
            e.printStackTrace();
        } finally {
            try { close(); } catch (IOException e) { }
        }
    }



    @Override
    public void close() throws IOException {
        super.close();
        running = false;
    }
}
