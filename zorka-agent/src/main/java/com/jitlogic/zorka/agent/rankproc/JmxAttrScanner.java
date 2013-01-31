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

package com.jitlogic.zorka.agent.rankproc;

import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JMX Attribute Scanner is responsible for traversing JMX (using supplied queries) and
 * submitting obtained metric data to tracer output. Results will be sorted into categories
 * (integers, long integers, double precision FP), attribute sets will be concatenated and
 * converted to symbols, data of each category will be submitted.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class JmxAttrScanner implements Runnable {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(JmxAttrScanner.class);

    /** Scanner ID (attached to every packet of sample data) */
    private int id;

    /** Symbol registry */
    private SymbolRegistry symbols;

    /** Output handler - handles generated data (eg. saves them to trace files). */
    private TraceEventHandler output;

    /** Query listers representing queries supplied at scanner construction time */
    private List<QueryLister> listers = new ArrayList<QueryLister>();


    /**
     * Creates new JMX attribute scanner object.
     *
     * @param symbols symbol registry
     *
     * @param name scanner name (converted to ID using symbol registry and attached to every emitted packet of data).
     *
     * @param output tracer output
     *
     * @param registry MBean server registry object
     *
     * @param qdefs JMX queries
     */
    public JmxAttrScanner(SymbolRegistry symbols, String name, TraceEventHandler output, MBeanServerRegistry registry, QueryDef...qdefs) {
        this.symbols = symbols;
        this.id = symbols.symbolId(name);
        this.output = output;

        for (QueryDef qdef : qdefs) {
            this.listers.add(new QueryLister(registry, qdef));
        }
    }




    @Override
    public void run() {
        runCycle(System.currentTimeMillis());
    }


    /**
     * Performs one scan-submit cycle.
     *
     * @param tstamp current time (milliseconds since Epoch)
     */
    public void runCycle(long tstamp) {
        List<Integer> longIds = new ArrayList<Integer>(128);
        List<Long> longVals = new ArrayList<Long>(128);

        List<Integer> intIds = new ArrayList<Integer>(128);
        List<Integer> intVals = new ArrayList<Integer>(128);

        List<Integer> doubleIds = new ArrayList<Integer>(128);
        List<Double> doubleVals = new ArrayList<Double>(128);

        for (QueryLister lister : listers) {
            for (QueryResult result : lister.list()) {
                Object val = result.getValue();
                if (val instanceof Long) {
                    longIds.add(result.getComponentId(symbols));
                    longVals.add((Long)val);
                } else if (val instanceof Integer) {
                    intIds.add(result.getComponentId(symbols));
                    intVals.add((Integer)val);
                } else if (val instanceof Double) {
                    doubleIds.add(result.getComponentId(symbols));
                    doubleVals.add((Double)val);
                } else if (val instanceof Short) {
                    intIds.add(result.getComponentId(symbols));
                    intVals.add((int)(Short)val);
                } else if (val instanceof Byte) {
                    intIds.add(result.getComponentId(symbols));
                    intVals.add((int)(Byte)val);
                } else if (val instanceof Float) {
                    doubleIds.add(result.getComponentId(symbols));
                    doubleVals.add((double)(Float)val);
                }
            }
        } // for ()

        if (longIds.size() > 0) {
            output.longVals(tstamp, id, ZorkaUtil.intArray(longIds), ZorkaUtil.longArray(longVals));
        }

        if (intIds.size() > 0) {
            output.intVals(tstamp, id, ZorkaUtil.intArray(intIds), ZorkaUtil.intArray(intVals));
        }

        if (doubleIds.size() > 0) {
            output.doubleVals(tstamp, id, ZorkaUtil.intArray(doubleIds), ZorkaUtil.doubleArray(doubleVals));
        }
    }


}
