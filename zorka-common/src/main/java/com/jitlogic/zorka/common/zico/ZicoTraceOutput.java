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


import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceOutput;
import com.jitlogic.zorka.common.tracedata.TraceWriter;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ZicoTraceOutput extends ZorkaAsyncThread<SymbolicRecord> implements TraceOutput {

    private static ZorkaLog log = ZorkaLogger.getLog(ZicoTraceOutput.class);

    private String hostname;
    private String auth;

    private ZicoClientConnector conn;

    private TraceWriter writer;

    private ByteArrayOutputStream os;

    private int retries;
    private long retryTime, retryTimeExp;
    private long packetSize;


    public ZicoTraceOutput(TraceWriter writer, String addr, int port, String hostname, String auth,
                           int qlen, long packetSize, int retries, long retryTime, long retryTimeExp, int timeout) throws IOException {
        super("zico-output", qlen);

        this.hostname = hostname;
        this.auth = auth;

        this.retries = retries;
        this.retryTime = retryTime;
        this.retryTimeExp = retryTimeExp;
        this.packetSize = packetSize;

        conn = new ZicoClientConnector(addr, port, timeout);

        this.writer = writer;
        this.os = new ByteArrayOutputStream(512 * 1024);
        this.writer.setOutput(this);
    }


    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    /**
     * Submits object to a queue.
     *
     * @param obj object to be submitted
     */
    @Override
    public boolean submit(SymbolicRecord obj) {
        boolean submitted = false;
        try {
            submitted = submitQueue.offer(obj, 1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        if (!submitted) {
            AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_DROPPED);
        }

        return submitted;
    }

    @Override
    protected void process(SymbolicRecord record) {
        long rt = retryTime;

        List<SymbolicRecord> packet = new ArrayList<SymbolicRecord>();
        packet.add(record);

        log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Processing record: " + record);

        for (int i = 0; i < retries; i++) {
            try {
                if (!conn.isOpen()) {
                    log.debug(ZorkaLogger.ZTR_TRACER_DBG,
                            "Opening connection to " + conn.getAddr() + ":" + conn.getPort());
                    conn.connect();
                }

                os.reset();
                writer.softReset();

                for (SymbolicRecord rec : packet) {
                    writer.write(rec);
                }

                while (os.size() < packetSize && submitQueue.size() > 0) {
                    SymbolicRecord rec = submitQueue.take();
                    packet.add(rec);
                    writer.write(rec);
                }

                byte[] data = os.toByteArray();
                log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Sending ZICO packet: len=" + data.length);
                conn.send(ZicoPacket.ZICO_DATA, data);
                ZicoPacket rslt = conn.recv();
                log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Received response: status=" + rslt.getStatus());
                if (rslt.getStatus() != ZicoPacket.ZICO_OK) {
                    throw new ZicoException(rslt.getStatus(), "Error submitting data.");
                }
                AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_SENT);
                return;
            } catch (SocketTimeoutException e) {
                log.info(ZorkaLogger.ZCL_STORE, "Resetting collector connection.");
                this.close();
                this.open();
                AgentDiagnostics.inc(AgentDiagnostics.ZICO_RECONNECTS);
            } catch (Exception e) {
                log.error(ZorkaLogger.ZCL_STORE, "Error sending trace record: " + e + ". Resetting connection.");
                this.close();
                this.open();
                AgentDiagnostics.inc(AgentDiagnostics.ZICO_RECONNECTS);
            }

            try {
                log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Will retry (wait=" + rt + ")");
                Thread.sleep(rt);
            } catch (InterruptedException e) {

            }

            rt *= retryTimeExp;
        } // for ( ... )

        AgentDiagnostics.inc(AgentDiagnostics.ZICO_PACKETS_LOST);
        log.error(ZorkaLogger.ZCL_STORE, "Too many errors while trying to send trace. Giving up. Trace will be lost.");
    }

    /**
     * Single run cycle. For this particular class it needs
     */
    @Override
    protected void runCycle() {
        try {
            SymbolicRecord obj = submitQueue.take();
            if (obj != null) {
                process(obj);
            }
        } catch (InterruptedException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot perform run cycle", e);
        }
    }


    @Override
    protected void open() {
        try {
            writer.reset();
            conn.connect();
            conn.hello(hostname, auth);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZCL_STORE, "Error connecting " + conn.getAddr() + ":" + conn.getPort()
                    + ": " + e.getMessage() + "       (will reconnect later)");
        }
    }


    protected void close() {
        try {
            conn.close();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_STORE, "Error disconnecting " + conn.getAddr() + ":" + conn.getPort(), e);
        }
    }

}
