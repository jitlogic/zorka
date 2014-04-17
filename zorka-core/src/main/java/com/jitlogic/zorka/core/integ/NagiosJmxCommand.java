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

    public static final int SEL_SUM = 0;
    public static final int SEL_FIRST = 1;
    public static final int SEL_ONE = 2;

    public static final int RC_NONE = 0;
    public static final int RC_MIN = 1;
    public static final int RC_MAX = 2;

    private MBeanServerRegistry mBeanServerRegistry;
    private QueryDef query;

    private String template;

    private String label;
    private String[] pAttrs;
    private String pSuffix;


    private String nomAttr;
    private String divAttr;

    private int selMode;
    private String selName, selVal;

    int rcMode;
    double rcWarn;
    double rcAlrt;


    public NagiosJmxCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef query,
                            String template, String label, String[] pAttrs,
                            String nomAttr, String divAttr) {

        this.mBeanServerRegistry = mBeanServerRegistry;
        this.query = query.with(QueryDef.NO_NULL_ATTRS);

        this.template = template;
        this.label = label;
        this.pAttrs = pAttrs;

        this.nomAttr = nomAttr;
        this.divAttr = divAttr;

    }


    public NagiosJmxCommand rcByMin(double rcWarn, double rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MIN;
        return this;
    }


    public NagiosJmxCommand rcByMax(double rcWarn, double rcAlrt) {
        this.rcWarn = rcWarn;
        this.rcAlrt = rcAlrt;
        this.rcMode = RC_MAX;
        return this;
    }


    public NagiosJmxCommand selSum() {
        this.selMode = SEL_SUM;
        return this;
    }


    public NagiosJmxCommand selectFirst() {
        this.selMode = SEL_FIRST;
        return this;
    }


    public NagiosJmxCommand selectOne(String selName, String selVal) {
        this.selMode = SEL_ONE;
        this.selName = selName;
        this.selVal = selVal;
        return this;
    }


    private String[] perfLine(QueryResult result) {
        String plabel = ObjectInspector.substitute(label, result.getAttrs());
        StringBuilder pval = new StringBuilder();
        for (String pAttr : pAttrs) {
            Object v = ObjectInspector.get(result.getValue(), pAttr);
            if (v == null) { v = ""; }
            if (pval.length() != 0) {
                pval.append(";");
                pval.append(v);
            } else {
                pval.append(v);
                pval.append(pSuffix);
            }
        }
        return new String[] { plabel, pval.toString() };
    }


    private Map<String,Long> sumStats(List<QueryResult> input) {
        Map<String,Long> sum = new HashMap<String, Long>();

        for (String a : pAttrs) {
            sum.put(a, 0L);
        }

        for (QueryResult result : input) {
            for (String pa : pAttrs) {
                Object v = ObjectInspector.get(result.getValue(), pa);
                if (v instanceof Long) {
                    sum.put(pa, sum.get(pa)+((Long)v));
                } else if (v instanceof Integer) {
                    long l = (long)(Integer)v;
                    sum.put(pa, sum.get(pa)+l);
                }
            }
        }

        return sum;
    }


    public int resultCode(double v) {
        return NagiosLib.OK;
    }


    @Override
    public NrpePacket cmd(Object... args) {

        List<String[]> perfdata = new ArrayList<String[]>();
        List<QueryResult> results = new QueryLister(mBeanServerRegistry, query).list();

        if (label != null) {
            for (QueryResult result : results) {
                perfdata.add(perfLine(result));
            }
        }

        Map<String,Long> sum = null;
        QueryResult sel = null;

        switch (selMode) {
            case SEL_FIRST:
                if (results.size() > 0) {
                    sum = sumStats(results.subList(0,1));
                    sel = results.get(0);
                }
                break;
            case SEL_SUM:
                sum = sumStats(results);
                break;
            case SEL_ONE:
                for (int i = 0; i < results.size(); i++) {
                    if (selVal.equals(results.get(i).getAttr(selName))) {
                        sum = sumStats(results.subList(i,i+1));
                        sel = results.get(i);
                    }
                }
                break;
        }

        Map<String,Object> firstLineAttrs = new HashMap<String, Object>();
        if (sel != null) {
            firstLineAttrs.putAll(sel.getAttrs());
        }

        int rcode = NagiosLib.OK;

        if (sum != null) {
            firstLineAttrs.putAll(sum);

            if (nomAttr != null && divAttr != null) {
                Long nv = sum.get(nomAttr), dv = sum.get(divAttr);
                if (nv != null && dv != null) {
                    double pct = 100.0 * nv / dv;
                    firstLineAttrs.put("%", pct);
                    rcode = resultCode(pct);
                } else if (nv != null) {
                    rcode = resultCode((double)nv);
                }
            }
        }

        String firstLine = ObjectInspector.substitute(template, firstLineAttrs);

        StringBuilder pktContent = new StringBuilder();
        pktContent.append(firstLine);

        if (label != null && sum != null) {
            String sumLabel = sel != null ? ObjectInspector.substitute(label, sel.getAttrs()) : label;
            StringBuilder sumValue = new StringBuilder();
            for (String pa : pAttrs) {
                Object v = sum.get(pa) != null ? sum.get(pa) : "";
                if (sumValue.length() == 0) {
                    sumValue.append(v);
                    sumValue.append(pSuffix);
                } else {
                    sumValue.append(';');
                    sumValue.append(v);
                }
            }

        }


        return NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, rcode, pktContent.toString());
    }
}
