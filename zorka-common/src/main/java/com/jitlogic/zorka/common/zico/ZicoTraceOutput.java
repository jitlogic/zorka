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


import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceOutput;
import com.jitlogic.zorka.common.tracedata.TraceWriter;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ZicoTraceOutput extends ZorkaAsyncThread<SymbolicRecord> implements TraceOutput {

    private static ZorkaLog log = ZorkaLogger.getLog(ZicoTraceOutput.class);

    private String hostname;
    private String auth;

    private ZicoClientConnector conn;

    private TraceWriter writer;

    private ByteArrayOutputStream os;


    public ZicoTraceOutput(TraceWriter writer, String addr, int port, String hostname, String auth) throws IOException {
        super("zico-output", 64);

        this.hostname = hostname;
        this.auth = auth;

        conn = new ZicoClientConnector(addr, port);

        this.writer = writer;
        this.os = new ByteArrayOutputStream(512 * 1024);
        this.writer.setOutput(this);
    }


    @Override
    public OutputStream getOutputStream() {
        return os;
    }


    @Override
    protected void process(SymbolicRecord record) {
        try {
            os.reset();
            writer.softReset();
            writer.write(record);
            conn.send(ZicoPacket.ZICO_DATA, os.toByteArray());
            ZicoPacket rslt = conn.recv();
            if (rslt.getStatus() != ZicoPacket.ZICO_OK) {
                throw new ZicoException(rslt.getStatus(), "Error submitting data.");
            }
        } catch (Exception e) {
            log.error(ZorkaLogger.ZCL_STORE, "Error sending trace record. Resetting connection.", e);
            this.close();
            this.open();
            // TODO push record back to a queue
        }
    }


    @Override
    protected void open() {
        try {
            writer.reset();
            conn.connect();
            conn.hello(hostname, auth);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZCL_STORE, "Error connecting " + conn.getAddr() + ":" + conn.getPort(), e);
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
