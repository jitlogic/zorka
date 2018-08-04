/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Nagios library functions.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class NagiosLib {

    private static final Logger log = LoggerFactory.getLogger(NagiosLib.class);

    /** OK status */
    public static final int OK = NrpePacket.OK;

    /** WARNING status */
    public static final int WARN = NrpePacket.WARN;

    /** ERROR status */
    public static final int ERROR = NrpePacket.ERROR;

    /** UNKNOWN status */
    public static final int UNKNOWN = NrpePacket.UNKNOWN;


    private Map<String,NagiosCommand> commands = new ConcurrentHashMap<String, NagiosCommand>();
    private MBeanServerRegistry mBeanServerRegistry;


    public NagiosLib(MBeanServerRegistry mBeanServerRegistry) {
        this.mBeanServerRegistry = mBeanServerRegistry;
    }


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
    public NrpePacket reply(int resultCode, String title, String format, Object... vals) {
        String msg = "" + title + new Date() + "|" + String.format(format, vals);
        return NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, resultCode, msg);
    }


    /**
     * Creates error NRPE response.
     * @param msg error message
     * @return NRPE response.
     */
    public NrpePacket error(String msg) {
        return NrpePacket.error(msg);
    }


    Pattern RE_SPLIT = Pattern.compile("/");

    private String[] splitArgs(String lstr) {
        if (lstr != null && lstr.length() > 0) {
            return RE_SPLIT.split(lstr);
        }
        return new String[0];
    }


    public NagiosJmxCommand jmxscan(QueryDef...qdefs) {
        return new NagiosJmxCommand(mBeanServerRegistry, qdefs);
    }


    public void defcmd(String id, NagiosCommand cmd) {

        if (commands.containsKey(id)) {
            log.warn("Redefining already defined nagios command '" + id + "'");
        }

        log.info("Definig Nagios command: " + id);

        commands.put(id, cmd);
    }


    /**
     * Executes predefined command.
     *
     * @param id command name
     * @param args arguments (optional)
     *
     * @return NRPE response.
     */
    public NrpePacket cmd(String id, Object...args) {
        NagiosCommand cmd = commands.get(id);

        if (cmd == null) {
            return error("No such command: '" + id + "'");
        }

        return cmd.cmd(args);
    }
}
