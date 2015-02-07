/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.ZorkaLib;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Some old stuff to calculate average rates. To be removed soon.
 */
public class AvgRateCounter {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    public static final long AVG1 = 60 * 1000;
    public static final long AVG5 = 5 * 60 * 1000;
    public static final long AVG15 = 15 * 60 * 1000;

    /**
     * Zorka library reference
     */
    private ZorkaLib zorkaLib;

    /**
     * All maintained aggregates
     */
    private Map<String, RateAggregate> aggregates;

    /**
     * Creates rate counter
     *
     * @param zorkaLib
     */
    public AvgRateCounter(ZorkaLib zorkaLib) {
        this.zorkaLib = zorkaLib;
        aggregates = new ConcurrentHashMap<String, RateAggregate>();
    }

    /**
     * Returns calculated average
     */
    public double get(List<Object> path, String nomAttr, String divAttr, long horizon) {

        String tag = makeTag(path, nomAttr, divAttr, horizon);

        RateAggregate aggregate = aggregates.get(tag);

        if (aggregate == null) {
            aggregate = new RateAggregate(horizon, 0.0);
            aggregates.put(tag, aggregate);
            AgentDiagnostics.inc(AgentDiagnostics.AVG_CNT_CREATED);
        }

        synchronized (aggregate) {
            Object obj = zorkaLib.jmx(path.toArray(new Object[0]));
            Object nom = ObjectInspector.get(obj, nomAttr);
            Object div = divAttr != null ? ObjectInspector.get(obj, divAttr) : 0;

            aggregate.feed(coerce(nom), coerce(div));

            return aggregate.rate();
        }
    }

    /**
     * Coerces values
     */
    public long coerce(Object val) {
        if (val instanceof Long) {
            return (Long) val;
        } else if (val instanceof Integer) {
            return (long) (Integer) val;
        } else if (val instanceof Short) {
            return (long) (Short) val;
        } else if (val instanceof Byte) {
            return (long) (Byte) val;
        } else {
            AgentDiagnostics.inc(AgentDiagnostics.AVG_CNT_ERRORS);
            return 0;
        }
    }

    /**
     * Generates identifier for aggregates map
     */
    private String makeTag(List<Object> path, String nomAttr, String divAttr, long horizon) {
        StringBuilder sb = new StringBuilder(128);

        for (Object s : path) {
            sb.append(s);
            sb.append("::");
        }

        sb.append(nomAttr);
        sb.append("::");
        sb.append(divAttr);
        sb.append("::");
        sb.append(horizon);

        return sb.toString();
    }


    /**
     * Creates a list of strings
     */
    public List<Object> list(Object... strings) {
        List<Object> lst = new ArrayList<Object>(strings.length + 2);
        for (Object s : strings) {
            lst.add(s);
        }
        return lst;
    }

}
