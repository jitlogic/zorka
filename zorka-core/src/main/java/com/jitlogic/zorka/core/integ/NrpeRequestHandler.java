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
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;

import java.io.IOException;
import java.net.Socket;

/**
 * Handles Nagios NRPE requests and passes them to BSH agent.
 */
public class NrpeRequestHandler implements ZorkaRequestHandler {

    /**
     * Logger
     */
    private static final ZorkaLog log = ZorkaLogger.getLog(NrpeRequestHandler.class);

    /**
     * Request packet
     */
    private NrpePacket req;

    /**
     * Accepted connection socket
     */
    private Socket socket;

    /**
     * Request handling start timestamp.
     */
    private long tStart;

    /**
     * Request handling finish timestamp
     */
    private long tStop;

    /**
     * Query translator
     */
    private QueryTranslator translator;

    /**
     * Creates NRPE request handler object
     *
     * @param socket accepted connection socket
     */
    public NrpeRequestHandler(Socket socket, QueryTranslator translator) {
        this.socket = socket;
        this.tStart = System.nanoTime();
        this.translator = translator;

        AgentDiagnostics.inc(AgentDiagnostics.NAGIOS_REQUESTS);
    }


    @Override
    public void handleResult(Object rslt) {
        tStop = System.nanoTime();
        log.debug(ZorkaLogger.ZAG_QUERIES, "OK [t=" + (tStop - tStart) / 1000000L + "ms] '" + req + "' -> '" + rslt + "'");

        AgentDiagnostics.inc(AgentDiagnostics.NAGIOS_TIME, tStop - tStart);

        if (req == null) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error: nagios request is null (propably parse error or deadlock)");
            try {
                socket.close();
            } catch (IOException e) {
            }
            return;
        }

        NrpePacket resp = null;
        if (rslt instanceof NrpePacket) {
            NrpePacket pkt = (NrpePacket) rslt;
            resp = req.createResponse(pkt.getResultCode(), pkt.getData());
        } else {
            resp = req.createResponse((short) 0, "" + rslt);
        }

        try {
            resp.encode(socket.getOutputStream());
            socket.getOutputStream().flush();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error sending NRPE response", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error closing network socket", e);
            }
        }
    }


    @Override
    public void handleError(Throwable e) {
        AgentDiagnostics.inc(AgentDiagnostics.NAGIOS_ERRORS);
        tStop = System.nanoTime();
        log.debug(ZorkaLogger.ZAG_QUERIES, "ERROR [t=" + (tStop - tStart) / 1000000L + "ms] '" + req + "' -> '", e);

        AgentDiagnostics.inc(AgentDiagnostics.NAGIOS_TIME, tStop - tStart);

        NrpePacket resp = req.createResponse(3, "Error: " + e);
        try {
            resp.encode(socket.getOutputStream());
        } catch (IOException e1) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error sending NRPE response", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e2) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error closing network socket", e);
            }
        }
    }


    @Override
    public String getReq() {

        if (req == null) {
            try {
                req = NrpePacket.fromStream(socket.getInputStream());
            } catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error parsing NRPE packet", e);
                return "";
            }
        }

        return translator.translate(req.getData());
    }
}
