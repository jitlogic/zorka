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

package com.jitlogic.zorka.integ;

import java.util.Date;

/**
 * Nagios library functions.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class NagiosLib {

    /** OK status */
    public static final int OK = 0;

    /** WARNING status */
    public static final int WARN = 1;

    /** ERROR status */
    public static final int ERROR = 2;

    /** UNKNOWN status */
    public static final int UNKNOWN = 3;

    /**
     * Creates reply packet.
     *
     * @param resultCode result code
     *
     * @param title title
     *
     * @param format message template
     *
     * @param vals values used to fill template placeholders
     *
     * @return NRPE packet object
     */
    private NrpePacket reply(int resultCode, String title, String format, Object... vals) {
        String msg = "" + title + new Date() + "|" + String.format(format, vals);
        NrpePacket reply = NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, resultCode, msg);
        return reply;
    }

}
