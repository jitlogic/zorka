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
package com.jitlogic.zorka.integ.syslog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyslogLib {


    // Severity codes
    public final static int S_EMERGENCY = 0;
    public final static int S_ALERT = 1;
    public final static int S_CRITICAL = 2;
    public final static int S_ERROR = 3;
    public final static int S_WARNING = 4;
    public final static int S_NOTICE = 5;
    public final static int S_INFO = 6;
    public final static int S_DEBUG = 7;


    // Facilities
    public final static int F_KERNEL = 0;
    public final static int F_USER = 1;
    public final static int F_MAIL = 2;
    public final static int F_SYSTEM = 3;
    public final static int F_AUTH1 = 4;
    public final static int F_SYSLOG = 5;
    public final static int F_PRINTER = 6;
    public final static int F_NETWORK = 7;
    public final static int F_UUCP = 8;
    public final static int F_CLOCK1 = 9;
    public final static int F_AUTH2 = 10;
    public final static int F_FTPD = 11;
    public final static int F_NTPD = 12;
    public final static int F_AUDIT = 13;
    public final static int F_ALERT = 14;
    public final static int F_CLOCK2 = 15;
    public final static int F_LOCAL0 = 16;
    public final static int F_LOCAL1 = 17;
    public final static int F_LOCAL2 = 18;
    public final static int F_LOCAL3 = 19;
    public final static int F_LOCAL4 = 20;
    public final static int F_LOCAL5 = 21;
    public final static int F_LOCAL6 = 22;
    public final static int F_LOCAL7 = 23;


    private Map<String,SyslogTrapper> trappers = new ConcurrentHashMap<String, SyslogTrapper>();


    private static String[] facilities = {
        "F_KERNEL", "F_USER",   "F_MAIL",   "F_SYSTEM", "F_AUTH1",  "F_SYSLOG", "F_PRINTER", "F_NETWORK", // 0..7
        "F_UUCP",   "F_CLOCK1", "F_AHTN2",  "F_FTPD",   "F_NTPD",   "F_AUDIT",  "F_ALERT",   "F_CLOCK2",  // 8..15
        "F_LOCAL0", "F_LOCAL1", "F_LOCAL2", "F_LOCAL3", "F_LOCAL4", "F_LOCAL5", "F_LOCAL6",  "F_LOCAL7"   // 16..23
    };


    public static int getFacility(String name) {
        for (int i = 0; i < facilities.length; i++) {
            if (facilities[i].equals(name))
                return i;
        }

        return F_LOCAL0;
    }


    public SyslogTrapper trapper(String id) {
        return trappers.get(id);
    }


    public SyslogTrapper trapper(String id, String syslogServer, String defaultHost) {
        SyslogTrapper trapper = trappers.get(id);
        if (trapper == null) {
            trapper = new SyslogTrapper(syslogServer, defaultHost);
            trappers.put(id, trapper);
            trapper.start();
        }
        return trapper;
    }


    public void remove(String id) {
        SyslogTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.stop();
        }
    }


    public void log(String id, int severity, int facility, String tag, String content) {
        SyslogTrapper trapper = trappers.get(id);

        if (trapper != null) {
            trapper.log(severity, facility,  tag,  content);
        }
    }


}
