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
package com.jitlogic.zico.client;


import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;

import java.util.Date;

public class ClientUtil {

    private static final NumberFormat DURATION_SFORMAT = NumberFormat.getFormat("#####");
    private static final NumberFormat DURATION_MFORMAT = NumberFormat.getFormat("###.00");

    public static final DateTimeFormat TSTAMP_FORMAT0 = DateTimeFormat.getFormat("yyyy-MM-dd");
    public static final DateTimeFormat TSTAMP_FORMAT1 = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormat TSTAMP_FORMAT2 = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public static String formatTimestamp(Long clock) {
        return TSTAMP_FORMAT2.format(new Date(clock));
    }

    public static String formatDuration(Long time) {
        double t = 1.0 * time / 1000000.0;
        String u = "ms";

        if (t > 1000.0) {
            t /= 1000.0;
            u = "s";
        }

        return t > 10 ? DURATION_SFORMAT.format(t) + u : DURATION_MFORMAT.format(t) + u;
    }

    public static long parseTimestamp(String tstamp, String hms) {
        if (tstamp == null || tstamp.trim().length() == 0) {
            return 0;
        }

        String tst = tstamp.trim();


        if (tst.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
            return TSTAMP_FORMAT2.parseStrict(tst).getTime();
        } else if (tst.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return TSTAMP_FORMAT1.parseStrict(tst).getTime();
        } else if (tst.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return TSTAMP_FORMAT0.parseStrict(tst).getTime();
        }

        return 0;
    }
}
