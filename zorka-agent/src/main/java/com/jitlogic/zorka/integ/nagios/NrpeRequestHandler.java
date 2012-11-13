/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.integ.nagios;

import com.jitlogic.zorka.agent.ZorkaCallback;
import com.jitlogic.zorka.integ.zabbix.ZabbixRequestHandler;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.io.IOException;
import java.net.Socket;

public class NrpeRequestHandler implements ZorkaCallback {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private NrpePacket req = null;
    private Socket socket;
    private volatile long tStart, tStop;


    public NrpeRequestHandler(Socket socket) {
        this.socket = socket;
        this.tStart = System.currentTimeMillis();

    }


    public void handleResult(Object rslt) {
        tStop = System.currentTimeMillis();
        log.debug("OK [t=" + (tStop-tStart) + "ms] '" + req + "' -> '" + rslt + "'");

        NrpePacket resp = null;
        if (rslt instanceof NrpePacket) {
            NrpePacket pkt = (NrpePacket)rslt;
            resp = req.createResponse(pkt.getResultCode(), pkt.getData());
        } else {
            resp = req.createResponse((short)0, ""+rslt);
        }

        try {
            resp.encode(socket.getOutputStream());
            socket.getOutputStream().flush();
        } catch (IOException e) {
            log.error("Error sending NRPE response", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing network socket", e);
            }
        }
    }


    public void handleError(Throwable e) {
        tStop = System.currentTimeMillis();
        log.debug("OK [t=" + (tStop-tStart) + "ms] '" + req + "' -> '", e);
        NrpePacket resp = req.createResponse(3, "Error: " + e);

        try {
            resp.encode(socket.getOutputStream());
        } catch (IOException e1) {
            log.error("Error sending NRPE response", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e2) {
                log.error("Error closing network socket", e);
            }
        }
    }


    public String getReq() {

        if (req == null) {
            try {
                req = NrpePacket.fromStream(socket.getInputStream());
            } catch (IOException e) {
                return ""; // TODO log it somewhere ?
            }
        }

        return ZabbixRequestHandler.translate(req.getData());
    }
}
