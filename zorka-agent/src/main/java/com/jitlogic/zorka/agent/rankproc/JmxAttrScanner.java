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

import com.jitlogic.zorka.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JmxAttrScanner implements Runnable {

    private static final ZorkaLog log = ZorkaLogger.getLog(JmxAttrScanner.class);

    private int id;
    private SymbolRegistry symbols;
    private TraceEventHandler output;

    private List<QueryLister> listers = new ArrayList<QueryLister>();


    public JmxAttrScanner(SymbolRegistry symbols, String name, TraceEventHandler output, QueryLister...listers) {
        this.symbols = symbols;
        this.id = symbols.symbolId(name);
        this.output = output;

        add(listers);
    }


    public void add(QueryLister...listers) {
        this.listers.addAll(Arrays.asList(listers));
    }


    @Override
    public void run() {
        runCycle(System.currentTimeMillis());
    }


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
