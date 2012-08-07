/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.rankproc;

import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import javax.management.MBeanServerConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AvgRateCounter {

    private static final ZorkaLogger log = ZorkaLogger.getLogger(AvgRateCounter.class);

    public final static long AVG1  =      60 * 1000;
    public final static long AVG5  =  5 * 60 * 1000;
    public final static long AVG15 = 15 * 60 * 1000;

    private ZorkaLib zorkaLib;
    private Map<String,RateAggregate> aggregates;


	public AvgRateCounter(ZorkaLib zorkaLib) {
        this.zorkaLib = zorkaLib;
		aggregates = new ConcurrentHashMap<String, RateAggregate>();
	}


    public double get(List<Object> path, String nomAttr, String divAttr, long horizon) {

        String tag = makeTag(path, nomAttr, divAttr, horizon);

        RateAggregate aggregate = aggregates.get(tag);

        if (aggregate == null) {
            aggregate = new RateAggregate(horizon, 0.0);
            aggregates.put(tag, aggregate);
        }

        synchronized(aggregate) {
            Object obj = zorkaLib.jmx(path.toArray(new Object[0]));
            Object nom = zorkaLib.get(obj, nomAttr);
            Object div = divAttr != null ? zorkaLib.get(obj, divAttr) : 0;

            aggregate.feed(coerce(nom), coerce(div));

            return aggregate.rate();
        }
    }


    public long coerce(Object val) {
        if (val instanceof Long) {
            return (Long)val;
        } else if (val instanceof Integer) {
            return (long)(Integer)val;
        } else if (val instanceof Short) {
            return (long)(Short)val;
        } else if (val instanceof Byte) {
            return (long)(Byte)val;
        } else {
            log.error("Cannot coerce object of type '"
                    + (val != null ? val.getClass().getName() : "null") + "' to Long.");
            return 0;
        }
    }


    private String makeTag(List<Object> path, String nomAttr, String divAttr, long horizon) {
        StringBuilder sb = new StringBuilder(128);

        for (Object s : path) {
            sb.append(s); sb.append("::");
        }

        sb.append(nomAttr);
        sb.append("::");
        sb.append(divAttr);
        sb.append("::");
        sb.append(horizon);

        return sb.toString();
    }


    public List<Object> list(Object...strings) {
        List<Object> lst = new ArrayList<Object>(strings.length);
        for (Object s : strings) {
            lst.add(s);
        }
        return lst;
    }

}
