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

import java.util.Date;

public class NagiosLib {


    private NrpePacket reply(int resultCode, String title, String format, Object... vals) {
        String msg = "" + title + new Date() + "|" + String.format(format, vals);
        NrpePacket reply = NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, resultCode, msg);
        return reply;
    }


    public NrpePacket ok(String title, String format, Object...vals) {
        return reply(0, title, format, vals);
    }


    public NrpePacket warn(String title, String format, Object...vals) {
        return reply(1, title, format, vals);
    }


    public NrpePacket error(String title, String format, Object...vals) {
        return reply(2, title, format, vals);
    }


    public NrpePacket unknown(String title, String format, Object...vals) {
        return reply(3, title, format, vals);
    }


}
