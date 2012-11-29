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
package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.integ.syslog.SyslogLogger;
import com.jitlogic.zorka.spy.SpyCollector;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;

public class SyslogCollector implements SpyCollector {

    private SyslogLogger logger;
    private String expr;
    int severity, facility;
    private String tag, hostname;

    private ObjectInspector inspector = new ObjectInspector();


    public SyslogCollector(SyslogLogger logger, String expr,
                           int severity, int facility, String hostname, String tag) {
        this.logger = logger;
        this.expr = expr;

        this.severity = severity;
        this.facility = facility;

        this.tag = tag;
        this.hostname = hostname;
    }


    public void collect(SpyRecord record) {
        Object[] vals = record.getVals(SpyLib.ON_COLLECT);

        String msg = inspector.substitute(expr, vals);

        if (hostname != null) {
            logger.log(severity, facility, hostname, tag, msg);
        } else {
            logger.log(severity,  facility, tag,  msg);
        }
    }
}
