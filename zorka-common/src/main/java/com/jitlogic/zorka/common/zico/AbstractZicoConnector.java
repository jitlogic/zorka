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
import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;


/**
 * ZICO stands for Zorka Internet Collector, Zorka Internal Communication (or whatever).
 *
 * Standard TCP port for ZICO communication is 8640 (0x21C0 - 'ZICO' in hex).
 *
 * ZICO message:
 * - zico magic   (2 bytes) -> 0x21C0
 * - message type (2 bytes) -> message type and checksum options;
 * - payload length (4 bytes) -> payload length;
 * - checksum (4 bytes) -> CRC32 of message content;
 * - message content -> to be decoded by other layers (might be zero length);
 */
public abstract class AbstractZicoConnector implements Closeable {

    /** */
    public final static int HEADER_LENGTH = 10;

    public final static int[] ZICO_MAGIC = { 0x21, 0xC0, 0xBA, 0xBE };

    /** Trace data payload. */
    public final static short ZICO_DATA = 0x0001;

    /** OK status. */
    public final static int ZICO_OK            = 0x0000;

    /** Malformed ZICO message. */
    public final static int ZICO_FORMAT_ERROR  = 0x0002;

    /** CRC Error. */
    public final static int ZICO_CRC_ERROR     = 0x0003;

    /** PING request */
    public final static int ZICO_PING = 0x0004;

    /** PING reply */
    public final static int ZICO_PONG = 0x0005;

    public final static int ZICO_AUTH_ERROR = 0x0006;

    protected InetAddress addr;
    protected int port;

    protected Socket socket;
    protected InputStream in;
    protected OutputStream out;

    //private ZicoDataProcessor context;

    protected AbstractZicoConnector() {

    }

    public AbstractZicoConnector(String addr, int port) throws IOException {
        this.addr = InetAddress.getByName(addr);
        this.port = port;
    }


    public AbstractZicoConnector(Socket socket, ZicoDataProcessor context) {
        this.socket = socket;
        //this.context = context;
    }


    public static void seekSignature(InputStream is, int...signature) throws IOException {
        int[] data = new int[signature.length];
        int n = 0;

        for (int i = 0; i < signature.length; i++) {
            data[i] = is.read();
        }

        do {
            for (n = 0; n < signature.length; n++) {
                if (data[n] != signature[n]) {
                    for (int i = 1; i < data.length; i++) {
                        data[i-1] = data[i];
                    }
                    data[data.length-1] = is.read();
                    break;
                }
            }
        } while (n < signature.length);
    }


    protected Object getMessage() throws IOException {

        seekSignature(in, ZICO_MAGIC);

        byte[] b = new byte[HEADER_LENGTH];
        in.read(b);

        ByteBuffer buf = ByteBuffer.wrap(b);

        short type = buf.getShort();
        int length = buf.getInt();
        long crc32 = buf.getInt() & 0xffffffffL;

        byte[] d = new byte[length];
        in.read(d);

        CRC32 crc = new CRC32();
        crc.update(d);

        if (crc32 != crc.getValue()) {
            sendMessage(ZICO_CRC_ERROR);
            return null;
        }

        switch (type) {
            case ZICO_PING:
                sendMessage(ZICO_PONG);
                break;
            case ZICO_DATA:
                try {
                    return unpackFressianData(d);
                } catch (Exception e) {
                    sendMessage(ZICO_FORMAT_ERROR);
                }
                break;
            default:
                return type;
        }

        return null;
    }


    private Object unpackFressianData(byte[] data) throws IOException {
        List<Object> lst = new ArrayList<Object>();
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        FressianReader r = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
        while (is.available() > 0) {
            lst.add(r.readObject());
        }
        return lst;
    }


    public short sendData(Object data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(data);
        writer.close();
        sendMessage(ZICO_DATA, os.toByteArray());
        Object ret = getMessage();
        if (ret instanceof Short) {
            return (Short)ret;
        } else {
            return ZICO_FORMAT_ERROR;
        }
    }


    public void sendMessage(int type, byte...data) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(data);

        for (int i : ZICO_MAGIC) {
            out.write(i);
        }

        byte[] b = new byte[HEADER_LENGTH];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putShort((short)type);
        buf.putInt(data.length);
        buf.putInt((int)(crc.getValue() & 0xffffffff));

        out.write(b);
        if (data.length > 0) {
            out.write(data);
        }
    }



    @Override
    public void close() throws IOException {
        if (socket != null) {
            in = null;
            out = null;
            socket.close();
            socket = null;
        }
    }
}
