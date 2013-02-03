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

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.spy.SpyInstance;
import com.jitlogic.zorka.common.*;

import java.util.List;

public class RankProcLib {

    /** Reference to symbol registry */
    private SymbolRegistry symbolRegistry;

    /** Reference to metrics registry */
    private MetricsRegistry metricsRegistry;


    public RankProcLib(SpyInstance instance) {
        this.symbolRegistry = instance.getTracer().getSymbolRegistry();
        this.metricsRegistry = instance.getTracer().getMetricsRegistry();
    }


    public MetricTemplate rawDataMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.RAW_DATA, name, units);
    }


    public MetricTemplate timedDeltaMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.TIMED_DELTA, name, units);
    }


    public MetricTemplate rawDeltaMetric(String name, String units) {
        return new MetricTemplate(MetricTemplate.RAW_DELTA, name, units);
    }


    public MetricTemplate windowedRateMetric(String name, String units, String nom, String div) {
        return new MetricTemplate(MetricTemplate.WINDOWED_RATE, name, units, nom, div);
    }


    public QueryDef query(String mbsName, String query, String... attrs) {
        return new QueryDef(mbsName, query, attrs);
    }

    /**
     * Creates new JMX metrics scanner object. Scanner objects are responsible for scanning
     * selected values accessible via JMX and
     *
     * @param name scanner name
     *
     * @param output output handler (eg. file)
     *
     * @param qdefs queries
     *
     * @return scanner object
     */
    public JmxAttrScanner scanner(String name, PerfDataEventHandler output, QueryDef... qdefs) {
        return new JmxAttrScanner(symbolRegistry, metricsRegistry, name,
                AgentInstance.getMBeanServerRegistry(), output, qdefs);
    }


    public JmxAttrScanner scanner(String name, final ZorkaAsyncThread<PerfRecord> output, QueryDef...qdefs) {
        return scanner(name,
            new PerfDataEventHandler() { // TODO clean up this mess !!
                @Override public void traceStats(long calls, long errors, int flags) { }
                @Override public void newSymbol(int symbolId, String symbolText) { }
                @Override public void newMetricTemplate(MetricTemplate template) { }
                @Override public void newMetric(Metric metric) { }
                @Override public void traceBegin(int traceId, long clock, int flags) { }
                @Override public void traceEnter(int classId, int methodId, int signatureId, long tstamp) { }
                @Override public void traceReturn(long tstamp) { }
                @Override public void traceError(Object exception, long tstamp) { }
                @Override public void newAttr(int attrId, Object attrVal) { }
                @Override public void perfData(long clock, int scannerId, List<PerfSample> samples) {
                    output.submit(new PerfRecord(clock, scannerId, samples));
                }
            }, qdefs);
    }
}
