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

public class NagiosJmxScanCommand extends AbstractNagiosCommand {

    protected static final ZorkaLog log = ZorkaLogger.getLog(NagiosJmxScanCommand.class);

    private JmxScanner scanner;
    private List<QueryLister> listers;


    public NagiosJmxScanCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef...qdefs) {
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


    public NrpePacket runScan(long clock) {

        Map<String, List<PerfSample>> results = fetchResults(clock);

        List<String> labels = new ArrayList<String>(results.size());
        Map<String,Map<String,Object>> rdata = new HashMap<String, Map<String, Object>>();

        for (Map.Entry<String,List<PerfSample>> e : results.entrySet()) {
            labels.add(e.getKey());
            rdata.put(e.getKey(), toRData(e.getKey(), e.getValue()));
        }

        Collections.sort(labels);

        Map<String,Object> sdata = null;

        switch (selMode) {
            // TODO SEL_SUM
            case SEL_FIRST:
                sdata = rdata.get(labels.get(0));
                labels.remove(0);
                break;
            case SEL_ONE:
                for (int i = 0; i < labels.size(); i++) {
                    String l = labels.get(i);
                    String rv = ObjectInspector.get(rdata, l, "ATTR", selName);
                    if (ZorkaUtil.objEquals(selVal, ""+rv)) {
                        sdata = rdata.get(l);
                        break;
                    }
                }
                break;
        }

        if (sdata == null) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Cannot calculate summary data for nagios request.");
            return NrpePacket.error("Cannot calculate summary data.");
        }

        sdata.put("STATUS", "OK"); // TODO calculate status properly

        String summary = ObjectInspector.substitute(tmplSummary, sdata);
        String perfLine = ObjectInspector.substitute(tmplPerfLine, sdata);

        String msg = summary + " | " + perfLine;

        return NrpePacket.response(NrpePacket.OK, msg);
    }


    @Override
    public NrpePacket cmd(Object... args) {
        return runScan(System.currentTimeMillis());
    }

}
