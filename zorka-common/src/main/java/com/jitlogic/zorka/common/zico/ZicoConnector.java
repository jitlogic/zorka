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


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;


/**
 * ZICO stands for Zorka Internet Collector, Zorka Internal Communication (or whatever).
 * <p/>
 * Standard TCP port for ZICO communication is 8640 (0x21C0 - 'ZICO' in hex).
 * <p/>
 * ZICO message:
 * - zico magic   (2 bytes) -> 0x21C0
 * - message type (2 bytes) -> message type and checksum options;
 * - payload length (4 bytes) -> payload length;
 * - checksum (4 bytes) -> CRC32 of message content;
 * - message content -> to be decoded by other layers (might be zero length);
 */
public abstract class ZicoConnector implements Closeable {

    /**
     * ZICO protocol header length (in bytes)
     */
    public final static int HEADER_LENGTH = 14;

    /**
     * ZICO protocol 'magic' signature
     */
    public final static int[] ZICO_MAGIC = {0x21, 0xC0, 0xBA, 0xBE};


    protected InetAddress addr;
    protected int port;

    protected Socket socket;
    protected InputStream in;
    protected OutputStream out;

    // TODO implement recv with timeout


    /**
     * Receives ZICO packet.
     *
     * @return unpacked object or status/error.
     * @throws IOException when communication error occurs.
     */
    protected ZicoPacket recv() throws IOException {

        // TODO this is not good design - encapsulate transferred packets and split data unpacking from data transfer

        for (int sbyte : ZICO_MAGIC) {
            int b;
            if ((b = in.read()) != sbyte) {
                if (b != -1) {
                    throw new ZicoException(ZicoPacket.ZICO_BAD_REPLY, "Malformed input data: invalid ZICO magic.");
                } else {
                    throw new ZicoException(ZicoPacket.ZICO_EOD, "Peer disconnected. Try again.");
                }
            }
        }

        byte[] b = new byte[HEADER_LENGTH];
        in.read(b);

        ByteBuffer buf = ByteBuffer.wrap(b);

        short type = buf.getShort();
        int length = buf.getInt();
        long crc32 = buf.getLong();

        byte[] d = new byte[length];
        int offs = 0;

        while (offs < length) {
            int rlen = in.read(d, offs, length - offs);
            if (rlen >= 0) {
                offs += rlen;
            } else {
                throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, "Unexpected end of stream.");
            }
        }

        CRC32 crc = new CRC32();
        crc.update(d);

        if (crc32 != crc.getValue()) {
            throw new ZicoException(ZicoPacket.ZICO_CRC_ERROR, "CRC error occured.");
        }

        return new ZicoPacket(type, d);
    }


    /**
     * Sends packet of given type.
     *
     * @param type packet type
     * @param data data (bytes)
     * @throws IOException when network error occurs.
     */
    public void send(int type, byte... data) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(data);

        for (int i : ZICO_MAGIC) {
            out.write(i);
        }

        byte[] b = new byte[HEADER_LENGTH];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putShort((short) type);
        buf.putInt(data.length);
        buf.putLong(crc.getValue());

        out.write(b);
        if (data.length > 0) {
            out.write(data);
        }
    }


    /**
     * Closes ZICO connection.
     *
     * @throws IOException when I/O error occurs while closing.
     */
    @Override
    public void close() throws IOException {
        if (socket != null) {
            in.close();
            in = null;
            out.close();
            out = null;
            socket.close();
            socket = null;
        }
    }


    public InetAddress getAddr() {
        return addr;
    }


    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }


    public int getPort() {
        return port;
    }


    public void setPort(int port) {
        this.port = port;
    }


    public boolean isOpen() {
        return socket != null;
    }
}
