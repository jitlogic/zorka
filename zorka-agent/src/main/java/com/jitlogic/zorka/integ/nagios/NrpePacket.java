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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class NrpePacket {

    public static final short QUERY_PACKET = 1;
    public static final short RESPONSE_PACKET = 2;

    private int version, type, resultCode;
    private String data;

    public static NrpePacket newInstance(int version, int type, int resultCode, String message) {
        NrpePacket pkt = new NrpePacket();

        pkt.version = version; pkt.type = type;
        pkt.resultCode = resultCode; pkt.data = message;

        return pkt;
    }

    public static NrpePacket fromStream(InputStream is) throws IOException {
        NrpePacket pkt = new NrpePacket();
        pkt.decode(is);
        return pkt;
    }


    private NrpePacket() { }


    public void decode(InputStream is) throws IOException {
        byte[] buf = new byte[1036];
        int len = is.read(buf);

        // Extract packet header
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        version = bb.getShort(); type = bb.getShort();
        byte[] crcBuf = new byte[8]; bb.get(crcBuf, 4, 4);
        long origCrc = ByteBuffer.wrap(crcBuf).order(ByteOrder.BIG_ENDIAN).getLong();
        resultCode = bb.getShort();

        System.arraycopy(crcBuf, 0, buf, 4, 4);

        CRC32 crc = new CRC32(); crc.update(buf, 0, len);

        if (crc.getValue() != origCrc) {
            throw new IOException("CRC error in received NRPE packet.");
        }

        int msglen = 0;

        for (int i = 10; i < 1036; i++) {
            if (buf[i] == 0) {
                msglen = i - 10;
                break;
            }
        }

        byte[] msg = new byte[msglen];
        bb.get(msg);

        data = new String(msg, "UTF-8");
    }


    public void encode(OutputStream os) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
        bb.putShort((short)version).putShort((short)type).putInt(0).putShort((short)resultCode);

        byte[] msg = data.getBytes();
        byte[] pkt = new byte[1036];

        System.arraycopy(bb.array(), 0, pkt, 0, 10);
        System.arraycopy(msg,  0, pkt, 10, msg.length);

        CRC32 crc = new CRC32(); crc.update(pkt);

        byte[] crc32 = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                .putLong(crc.getValue()).array();

        System.arraycopy(crc32, 4, pkt, 4, 4);
        pkt[pkt.length-1] = '\0';

        os.write(pkt);
    }


    public NrpePacket createResponse(int resultCode, String message) {
        return newInstance(version, type == QUERY_PACKET ? RESPONSE_PACKET : QUERY_PACKET, resultCode, message);
    }


    public int getVersion() {
        return version;
    }


    public int getType() {
        return type;
    }


    public int getResultCode() {
        return resultCode;
    }


    public String getData() {
        return data;
    }

}
