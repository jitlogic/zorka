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

import java.util.*;


public class NagiosJmxCommand implements NagiosCommand {

    /** Sum of all components in summary */
    public static final int SEL_SUM = 0;
    /** Choose first result as summary */
    public static final int SEL_FIRST = 1;
    /** Choose record by name as summary */
    public static final int SEL_ONE = 2;
    /** No RC calculation - always return OK */
    public static final int RC_NONE = 0;
    /** MIN RC calculation - alert when calculated utilization goes below threshold. */
    public static final int RC_MIN = 1;
    /** MAX RC calculation - alert when calculated utilization goes over threshold. */
    public static final int RC_MAX = 2;
    protected static final String[] RC_CODES = { "OK", "WARNING", "CRITICAL", "UNKNOWN" };
    /** MBean Server Registry. */
    protected MBeanServerRegistry mBeanServerRegistry;
    /** Suffix for retrieved performance data (eg. MB) */
    protected String pSuffix;
    /** Performance attribute names to be fetched. */
    protected List<String> pAttrs;
    /** Attribute used as label of measured objects in nagios result (eg. memory pool name) */
    protected String lblAttr;
    /** Nominal attribute for (calculated and reported) utilization. */
    protected String nomAttr;
    /** Divider attribute for (calculated and reported) utilization. */
    protected String divAttr;
    /** Determines how to scale results, eg. 1024 for kB, 1047576 for MB etc. */
    protected long scale = 1;
    /** Determines how result code should be calculated. See RC_* constants for details */
    protected int rcMode;
    /** Warning threshold - crossing it will result in WARNING status. */
    protected long rcWarn;
    /** Alert threshold - crossing it will result in CRITICAL status. */
    protected long rcAlrt;
    /** Summary selection mode (see SEL_* constants) */
    protected int selMode;
    /** When choosing selected result as summary: name of attribute by which result will be chosen. */
    protected String selName;
    /** When choosing selected result as summary: value of attribute by which result will be chosen. */
    protected String selVal;
    /** Tag that will be displayed in result summary. */
    protected String tag;
    /** More descriptive text appended to result summary. */
    protected String title;
    private QueryDef query;

    public NagiosJmxCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef query) {
        this.mBeanServerRegistry = mBeanServerRegistry;
        this.query = query.with(QueryDef.NO_NULL_ATTRS);
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

    /**
     * Sets scale for output data.
     *
     * @param scale      scale for results (eg. 1024 for K, 1048576 for M etc.)
     *
     * @return           altered command object
     */
    public NagiosJmxCommand withScale(long scale) {
        this.scale = scale;
        return this;
    }

    /**
     * Sets suffix for output data
     *
     * @param pSuffix    suffix for presented performance data (eg. MB);
     */
    public NagiosJmxCommand withSuffix(String pSuffix) {
        this.pSuffix = pSuffix;
        return this;
    }

    /**
     * Sets list of attributes to be retrieved from scanned objects.
     *
     * @param pAttrs attribute names
     */
    public NagiosJmxCommand withPerfAttrs(String... pAttrs) {
        this.pAttrs = Collections.unmodifiableList(Arrays.asList(pAttrs));
        return this;
    }

    public NagiosJmxCommand withAttrs(String lblAttr, String nomAttr, String divAttr) {
        this.lblAttr = lblAttr;
        this.nomAttr = nomAttr;
        this.divAttr = divAttr;

        if (this.pAttrs == null) {
            this.pAttrs = Collections.unmodifiableList(Arrays.asList(nomAttr, divAttr));
        }

        return this;
    }

    public NagiosJmxCommand withTitle(String tag, String title) {
        this.tag = tag;
        this.title = title;
        return this;
    }

    /**
     * Sets minimum alert thresholds.
     *
     * @param rcWarn warning level (WARN)
     *
     * @param rcAlrt alert level (CRITICAL)
     */
    public NagiosJmxCommand withRcMin(long rcWarn, long rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MIN;
        return this;
    }

    /**
     * Sets maximum alert thresholds.
     *
     * @param rcWarn warning level (WARN)
     *
     * @param rcAlrt alert level (CRITICAL)
     */
    public NagiosJmxCommand withRcMax(long rcWarn, long rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MAX;
        return this;
    }

    /**
     * Calculates sums of all results as summary.
     */
    public NagiosJmxCommand withSelSum() {
        this.selMode = SEL_SUM;
        return this;
    }

    /**
     * Chooses first result as summary.
     */
    public NagiosJmxCommand withSelFirst() {
        this.selMode = SEL_FIRST;
        return this;
    }

    /**
     * Selects specific result as summary
     *
     * @param selName attribute name to be checked
     *
     * @param selVal desired attribute value
     */
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

    protected String perfLine(QueryResult result, Map<String,Long> pstats) {
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
}
