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


public class ZicoPacket {

    public final static int ZICO_OK              = 0x0000;
    public final static int ZICO_DATA            = 0x0001;
    public final static int ZICO_CRC_ERROR       = 0x0003;
    public final static int ZICO_PING            = 0x0004;
    public final static int ZICO_PONG            = 0x0005;
    public final static int ZICO_AUTH_ERROR      = 0x0006;
    public final static int ZICO_INTERNAL_ERROR  = 0x0007;
    public final static int ZICO_BAD_REQUEST     = 0x0008;
    public final static int ZICO_BAD_REPLY       = 0x0009;
    public final static int ZICO_HELLO           = 0x0010;

    private int status;
    private byte[] data;

    public ZicoPacket(int status, byte[] data) {
        this.status = status;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
