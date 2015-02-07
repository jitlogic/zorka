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

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.util.ZorkaConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Syslog library manages syslog trappers and contains set of syslog-specific constants.
 * Syslog library is visible for zorka scripts as 'syslog' namespace.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SyslogLib implements ZorkaService {


    /**
     * EMERGENCY log level
     */
    public static final int S_EMERGENCY = 0;

    /**
     * ALERT log level
     */
    public static final int S_ALERT = 1;

    /**
     * CRITICAL log level
     */
    public static final int S_CRITICAL = 2;

    /**
     * ERROR log level
     */
    public static final int S_ERROR = 3;

    /**
     * WARNING log level
     */
    public static final int S_WARNING = 4;

    /**
     * NOTICE log level
     */
    public static final int S_NOTICE = 5;

    /**
     * IFNO log level
     */
    public static final int S_INFO = 6;

    /**
     * DEBUG log level
     */
    public static final int S_DEBUG = 7;


    /**
     * UNIX kernel messages
     */
    public static final int F_KERNEL = 0;

    /**
     * User messages
     */
    public static final int F_USER = 1;

    /**
     * Messages from MTAs
     */
    public static final int F_MAIL = 2;

    /**
     * Messages from system daemons
     */
    public static final int F_SYSTEM = 3;

    /**
     * Authentication, Authorization and Audit messages
     */
    public static final int F_AUTH1 = 4;

    /**
     * Messages from SYSLOG daemon
     */
    public static final int F_SYSLOG = 5;

    /**
     * Messages from lpd daemon
     */
    public static final int F_PRINTER = 6;

    /**
     * Messages from network services
     */
    public static final int F_NETWORK = 7;

    /**
     * Messages from UUCP services
     */
    public static final int F_UUCP = 8;

    /**
     * Messages from NTP daemon
     */
    public static final int F_CLOCK1 = 9;

    /**
     * Authentication, authorization and Audit messages
     */
    public static final int F_AUTH2 = 10;

    /**
     * Messages from FTP daemon
     */
    public static final int F_FTPD = 11;

    /**
     * Messages from NTPD
     */
    public static final int F_NTPD = 12;

    /**
     * Audit messages
     */
    public static final int F_AUDIT = 13;

    /**
     * Alerts
     */
    public static final int F_ALERT = 14;

    /**
     * Messages from NTPD
     */
    public static final int F_CLOCK2 = 15;

    /**
     * User messages
     */
    public static final int F_LOCAL0 = 16;

    /**
     * User messages
     */
    public static final int F_LOCAL1 = 17;

    /**
     * User messages
     */
    public static final int F_LOCAL2 = 18;

    /**
     * User messages
     */
    public static final int F_LOCAL3 = 19;

    /**
     * User messages
     */
    public static final int F_LOCAL4 = 20;

    /**
     * User messages
     */
    public static final int F_LOCAL5 = 21;

    /**
     * User messages
     */
    public static final int F_LOCAL6 = 22;

    /**
     * User messages
     */
    public static final int F_LOCAL7 = 23;

    /**
     * Keeps all syslog trappers created and registered by syslog library
     */
    private Map<String, SyslogTrapper> trappers = new ConcurrentHashMap<String, SyslogTrapper>();

    /**
     * Facility names
     */
    private static String[] facilities = {
            "F_KERNEL", "F_USER", "F_MAIL", "F_SYSTEM", "F_AUTH1", "F_SYSLOG", "F_PRINTER", "F_NETWORK", // 0..7
            "F_UUCP", "F_CLOCK1", "F_AHTN2", "F_FTPD", "F_NTPD", "F_AUDIT", "F_ALERT", "F_CLOCK2",  // 8..15
            "F_LOCAL0", "F_LOCAL1", "F_LOCAL2", "F_LOCAL3", "F_LOCAL4", "F_LOCAL5", "F_LOCAL6", "F_LOCAL7"   // 16..23
    };

    /**
     * Gets facility number based on name.
     *
     * @param name symbolic facility name
     * @return numeric facility ID
     */
    public static int getFacility(String name) {
        for (int i = 0; i < facilities.length; i++) {
            if (facilities[i].equals(name))
                return i;
        }

        return F_LOCAL0;
    }

    private ZorkaConfig config;

    public SyslogLib(ZorkaConfig config) {
        this.config = config;
    }


    /**
     * Returns syslog trapper (if already created and registered).
     *
     * @param id trapper unique ID (name)
     * @return trapper object or null if no trapper has been registered with such ID
     */
    public SyslogTrapper trapper(String id) {
        return trappers.get(id);
    }


    /**
     * Returns syslog trapper if already registered of created and registers a new one.
     *
     * @param id              trapper unique ID (name)
     * @param syslogServer    address and optional port of a syslog server (in address:port notation)
     * @param defaultHost     default host name for logged messages
     * @param defaultFacility default facility ID for logged messages
     * @return trapper object
     */
    public SyslogTrapper trapper(String id, String syslogServer, String defaultHost, int defaultFacility) {
        SyslogTrapper trapper = trappers.get(id);
        if (trapper == null) {
            trapper = new SyslogTrapper(config.formatCfg(syslogServer), config.formatCfg(defaultHost), defaultFacility);
            trappers.put(id, trapper);
            trapper.start();
        }
        return trapper;
    }


    /**
     * Stops and unregisters syslog trapper (if registered)
     *
     * @param id trapper unique ID (name)
     */
    public void remove(String id) {
        SyslogTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.close();
            trapper.stop();
        }


    }


    @Override
    public void shutdown() {

        for (SyslogTrapper trapper : trappers.values()) {
            trapper.close();
            trapper.stop();
        }

        trappers.clear();
    }


    /**
     * Sends message through a registered trapper (or ignores if no trapper with such name is available)
     *
     * @param id       trapper unique ID (name)
     * @param severity severity code (see S_* constants)
     * @param facility facility code (see F_* constants)
     * @param tag      message tag (eg. component name)
     * @param content  message text
     */
    public void log(String id, int severity, int facility, String tag, String content) {
        SyslogTrapper trapper = trappers.get(id);

        if (trapper != null) {
            trapper.log(severity, facility, tag, content);
        }
    }

}
