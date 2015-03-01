/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.tracedata.MetricTemplate;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.JmxScanner;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.perfmon.QueryLister;
import com.jitlogic.zorka.core.perfmon.QueryResult;

import java.util.*;

public class NagiosJmxCommand extends AbstractNagiosCommand {

    protected static final ZorkaLog log = ZorkaLogger.getLog(NagiosJmxCommand.class);

    private JmxScanner scanner;
    private List<QueryLister> listers;


    public NagiosJmxCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef... qdefs) {
        listers = new ArrayList<QueryLister>(qdefs.length);
        for (QueryDef qdef : qdefs) {
            this.listers.add(new QueryLister(mBeanServerRegistry, qdef));
        }
        this.scanner = new JmxScanner(mBeanServerRegistry, new MetricsRegistry(), new SymbolRegistry(), listers);
        scanner.setAttachResults(true);
    }


    private Map<String, List<PerfSample>> fetchResults(long clock) {
        Map<String,List<PerfSample>> results = new HashMap<String, List<PerfSample>>();

        // Grab and sort samples
        for (int ln = 0; ln < listers.size(); ln++) {
            QueryLister lister = listers.get(ln);
            for (PerfSample sample : scanner.getPerfSamples(clock, lister)) {
                QueryResult rslt = (QueryResult)sample.getResult();
                String label = ObjectInspector.substitute(tmplLabel, rslt.getAttrs());
                if (!results.containsKey(label)) {
                    results.put(label, new ArrayList<PerfSample>());
                }

                List<PerfSample> samples = results.get(label);

                while (samples.size() < ln) {
                    log.warn(ZorkaLogger.ZPM_ERRORS, "Non-aligned sample: " + sample);
                    samples.add(null);  // Make sure result are aligned properly ...
                }

                if (samples.size() > ln) {
                    log.error(ZorkaLogger.ZPM_ERRORS, "Overlapping sample when using label " + tmplLabel + ". " +
                            "old_sample=" + samples.get(samples.size()-1) + " new_sample=" + sample);
                }

                samples.add(sample);
            }
        }
        return results;
    }


    private Map<String,Object> toRData(String label, List<PerfSample> samples) {
        Map<String,Object> rdata = new HashMap<String, Object>();

        rdata.put("LABEL", label);

        for (int i = 0; i < samples.size(); i++) {
            PerfSample sample = samples.get(i);
            if (sample != null) {
                Number val = sample.getValue();
                rdata.put("LVAL"+i, val.longValue());
                rdata.put("DVAL"+i, val.doubleValue());
                QueryResult rslt = (QueryResult)sample.getResult();
                if (!rdata.containsKey("ATTR")) { rdata.put("ATTR", rslt.getAttrs()); }
                MetricTemplate mt = sample.getMetric().getTemplate();
                rdata.put("UNIT"+i, mt.getUnits());
            } else {
                rdata.put("LVAL"+i, 0L);
                rdata.put("DVAL"+i, 0.0);
            }
        }

        return rdata;
    }

    private Map<String,Object> sumRData(int ncols, Map<String,Map<String,Object>> rdata) {
        Map<String,Object> rslt = new HashMap<String, Object>();

        for (int i = 0; i < ncols; i++) {
            rslt.put("LVAL"+i, 0L);
            rslt.put("DVAL"+i, 0.0);
        }

        rslt.put("LABEL", selName);

        for (Map.Entry<String,Map<String,Object>> e : rdata.entrySet()) {
            Map<String,Object> v = e.getValue();
            for (int i = 0; i < ncols; i++) {
                String l = "LVAL"+i;
                if (v.containsKey(l)) {
                    rslt.put(l, (Long) rslt.get(l) + (Long) v.get(l));
                }
                String d = "DVAL"+i;
                if (v.containsKey(d)) {
                    rslt.put(d, (Double) rslt.get(d) + (Double) v.get(d));
                }
                String u = "UNIT"+i;
                if (v.containsKey(u) && !rslt.containsKey(u)) {
                    rslt.put(u, v.get(u));
                }
                if (v.containsKey("ATTR") && !rslt.containsKey("ATTR")) {
                    rslt.put("ATTR", v.get("ATTR"));
                }
            }
        }

        return rslt;
    }

    public int calcStatus(Map<String,Object> sdata) {

        if (rcMode == RC_NONE) {
            return NrpePacket.OK;
        }

        if (rcAttr == null) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Result calculation mode != NONE but reference attribute not configured.");
            return NrpePacket.OK;
        }

        if (!(sdata.get(rcAttr) instanceof Number)) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Result value `" + rcAttr + "` is not a number.");
            return NrpePacket.ERROR;
        }

        double v = ((Number) sdata.get(rcAttr)).doubleValue();


        if (rcMode == RC_MIN) {
            if (v < rcAlrt) {
                return NrpePacket.ERROR;
            }
            if (v < rcWarn) {
                return NrpePacket.WARN;
            }
        }

        if (rcMode == RC_MAX) {
            if (v > rcAlrt) {
                return NrpePacket.ERROR;
            }
            if (v > rcWarn) {
                return NrpePacket.WARN;
            }
        }

        return NrpePacket.OK;
    }





    public NrpePacket runScan(long clock) {

        Map<String, List<PerfSample>> results = fetchResults(clock);

        List<String> labels = new ArrayList<String>(results.size());
        Map<String,Map<String,Object>> rdata = new HashMap<String, Map<String, Object>>();

        int ncols = 0;
        for (Map.Entry<String,List<PerfSample>> e : results.entrySet()) {
            ncols = Math.max(ncols, e.getValue().size());
            labels.add(e.getKey());
            rdata.put(e.getKey(), toRData(e.getKey(), e.getValue()));
        }

        Collections.sort(labels);

        Map<String,Object> sdata = null;

        switch (selMode) {
            case SEL_SUM:
                sdata = sumRData(ncols, rdata);
                break;
            case SEL_FIRST:
                sdata = rdata.get(labels.get(0));
                labels.remove(0);
                break;
            case SEL_ONE:
                for (String l : labels) {
                    String rv = ObjectInspector.get(rdata, l, "ATTR", selName);
                    if (selVals.contains(""+rv)) {
                        sdata = rdata.get(l);
                        labels.remove(l);
                        break;
                    }
                }
                break;
        }

        if (sdata == null) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Cannot calculate summary data for nagios request.");
            return NrpePacket.error("Cannot calculate summary data.");
        }

        int status = calcStatus(sdata);

        sdata.put("STATUS", RC_CODES[status]);

        String summary = ObjectInspector.substitute(tmplSummary, sdata);
        String perfLine = ObjectInspector.substitute(tmplPerfLine, sdata);

        List<String> textLines = new ArrayList<String>(labels.size());
        List<String> perfLines = new ArrayList<String>(labels.size());

        for (String l : labels) {
            Map<String,Object> rd = rdata.get(l);
            textLines.add(ObjectInspector.substitute(tmplTextLine, rd));
            perfLines.add(ObjectInspector.substitute(tmplPerfLine, rd));
        }

        String msg = summary + "| " + perfLine;

        if (labels.size() > 0) {
            msg += "\n" + ZorkaUtil.join("\n", textLines) + "| " + ZorkaUtil.join("\n", perfLines);
        }

        return NrpePacket.response(status, msg);
    }


    @Override
    public NrpePacket cmd(Object... args) {
        return runScan(System.currentTimeMillis());
    }

}
