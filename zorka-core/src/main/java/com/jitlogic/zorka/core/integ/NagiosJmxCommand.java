/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.perfmon.QueryLister;
import com.jitlogic.zorka.core.perfmon.QueryResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NagiosJmxCommand implements NagiosCommand {

    private static final String[] RC_CODES = { "OK", "WARNING", "CRITICAL", "UNKNOWN" };

    public static final int SEL_SUM = 0;
    public static final int SEL_FIRST = 1;
    public static final int SEL_ONE = 2;

    public static final int RC_NONE = 0;
    public static final int RC_MIN = 1;
    public static final int RC_MAX = 2;

    private MBeanServerRegistry mBeanServerRegistry;
    private QueryDef query;

    private String tag, title;

    private long scale = 1;

    private String lblAttr;
    private String[] pAttrs;
    private String pSuffix;

    private String nomAttr;
    private String divAttr;

    private int selMode;
    private String selName, selVal;

    int rcMode;
    long rcWarn;
    long rcAlrt;


    public NagiosJmxCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef query, String tag, String title) {

        this.mBeanServerRegistry = mBeanServerRegistry;
        this.query = query.with(QueryDef.NO_NULL_ATTRS);

        this.tag = tag;
        this.title = title;
    }


    public NagiosJmxCommand withScale(long scale, String pSuffix) {
        this.scale = scale;
        this.pSuffix = pSuffix;
        return this;
    }


    public NagiosJmxCommand withPerfData(String pSuffix, String...pAttrs) {
        this.pSuffix = pSuffix;
        this.pAttrs = pAttrs;
        return this;
    }


    public NagiosJmxCommand withAttrs(String lblAttr, String nomAttr, String divAttr) {
        this.lblAttr = lblAttr;
        this.nomAttr = nomAttr;
        this.divAttr = divAttr;

        if (this.pAttrs == null) {
            this.pAttrs = new String[] { nomAttr, divAttr };
        }

        return this;
    }


    public NagiosJmxCommand withRcMin(long rcWarn, long rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MIN;
        return this;
    }


    public NagiosJmxCommand withRcMax(long rcWarn, long rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MAX;
        return this;
    }


    public NagiosJmxCommand withSelSum() {
        this.selMode = SEL_SUM;
        return this;
    }


    public NagiosJmxCommand withSelFirst() {
        this.selMode = SEL_FIRST;
        return this;
    }


    public NagiosJmxCommand withSelOne(String selName, String selVal) {
        this.selMode = SEL_ONE;
        this.selName = selName;
        this.selVal = selVal;
        return this;
    }


    public Map<String,Long> toPstats(QueryResult result) {
        Map<String,Long> pstats = new HashMap<String, Long>();

        for (String pa : pAttrs) {
            Object v = ObjectInspector.get(result.getValue(), pa);
            if (v instanceof Long) {
                pstats.put(pa, ((Long)v)/scale);
            } else if (v instanceof Integer) {
                pstats.put(pa, ((long)(Integer)v)/scale);
            } else {
                pstats.put(pa, 0L);
            }
        }

        return pstats;
    }


    public Map<String,Long> sumPstats(List<Map<String,Long>> pstatList) {
        Map<String,Long> sum = new HashMap<String, Long>();

        for (String a : pAttrs) {
            sum.put(a, 0L);
        }

        for (Map<String,Long> pstats : pstatList) {
            for (Map.Entry<String,Long> e : pstats.entrySet()) {
                sum.put(e.getKey(), sum.get(e.getKey())+e.getValue());
            }
        }

        return sum;
    }


    private String perfLine(QueryResult result, Map<String,Long> pstats) {
        StringBuilder sb = new StringBuilder();

        for (String pa : pAttrs) {
            if (sb.length() != 0) {
                sb.append(";");
                sb.append(pstats.get(pa));
            } else {
                sb.append(pstats.get(pa));
                sb.append(pSuffix);
            }
        }

        return (result != null ? result.getAttr(lblAttr) : "sum") + "=" + sb.toString();
    }


    public String textLine(QueryResult result, Map<String,Long> pstats) {
        StringBuilder sb = new StringBuilder();
        sb.append(result != null ? result.getAttr(lblAttr) : "sum");
        sb.append(' ');
        sb.append(pstats.get(nomAttr));
        sb.append(' ');
        sb.append(pSuffix);

        if (nomAttr != null && divAttr != null) {
            sb.append(' ');
            double pct = 100.0 * pstats.get(nomAttr) / pstats.get(divAttr);
            sb.append('(');
            sb.append((int)pct);
            sb.append("%)");
        }

        sb.append("; ");

        return sb.toString();
    }


    public int resultCode(Map<String,Long> pstats) {

        Long nv = pstats.get(nomAttr), dv = pstats.get(divAttr);
        if (divAttr != null && nv != null && dv != null) {
            double pct = 100.0 * nv / dv;
            nv = (long)pct;
        }

        if (nv == null) {
            return NrpePacket.UNKNOWN;
        }

        if (rcMode == RC_MIN) {
            if (nv < rcAlrt) {
                return NrpePacket.ERROR;
            }
            if (nv < rcWarn) {
                return NrpePacket.WARN;
            }
        }

        if (rcMode == RC_MAX) {
            if (nv > rcAlrt) {
                return NrpePacket.ERROR;
            }
            if (nv > rcWarn) {
                return NrpePacket.WARN;
            }
        }

        return NrpePacket.OK;
    }




    @Override
    public NrpePacket cmd(Object... args) {

        List<QueryResult> results = new QueryLister(mBeanServerRegistry, query).list();

        if (results.size() == 0) {
            return NrpePacket.error("No data found.");
        }

        List<Map<String,Long>> pstatList = new ArrayList<Map<String, Long>>();

        for (QueryResult result : results) {
            pstatList.add(toPstats(result));
        }

        Map<String,Long> sum = null;
        QueryResult sel = null;

        switch (selMode) {
            case SEL_FIRST:
                if (results.size() > 0) {
                    sum = pstatList.get(0);
                    sel = results.get(0);
                }
                break;
            case SEL_SUM:
                sum = sumPstats(pstatList);
                break;
            case SEL_ONE:
                for (int i = 0; i < results.size(); i++) {
                    if (selVal.equals(results.get(i).getAttr(selName))) {
                        sum = pstatList.get(i);
                        sel = results.get(i);
                    }
                }
                break;
        }

        int rcode = resultCode(sum);

        StringBuilder pktContent = new StringBuilder();

        // Tag, status and title for first result line
        pktContent.append(tag);
        pktContent.append(' ');
        pktContent.append(RC_CODES[rcode]);
        pktContent.append(" - ");
        pktContent.append(title);
        pktContent.append(" ");

        // Label and data for first line
        pktContent.append(textLine(sel, sum));

        pktContent.append("| ");
        pktContent.append(perfLine(sel, sum));

        for (int i = 0; i < results.size(); i++) {
            if (sel != results.get(i)) {
                pktContent.append('\n');
                pktContent.append(textLine(results.get(i), pstatList.get(i)));
            }
        }


        for (int i = 0; i < results.size(); i++) {
            if (sel != results.get(i)) {
                if (i > 0) {
                    pktContent.append('\n');
                } else {
                    pktContent.append("| ");
                }
                pktContent.append(perfLine(results.get(i), pstatList.get(i)));
            }
        }

        return NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, rcode, pktContent.toString());
    }
}
