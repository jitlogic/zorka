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

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.common.tracedata.MetricTemplate;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

public class PerfMonLib {

    /** Reference to symbol registry */
    private SymbolRegistry symbolRegistry;

    /** Reference to metrics registry */
    private MetricsRegistry metricsRegistry;

    private Tracer tracer;

    private MBeanServerRegistry mbsRegistry;

    public PerfMonLib(SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, Tracer tracer, MBeanServerRegistry mbsRegistry) {
        this.tracer = tracer;
        this.mbsRegistry = mbsRegistry;
        this.symbolRegistry = symbolRegistry;
        this.metricsRegistry = metricsRegistry;
    }


    public MetricTemplate metric(String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.RAW_DATA, name, description, units));
    }


    public MetricTemplate timedDelta(String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.TIMED_DELTA, name, description, units));
    }


    public MetricTemplate delta(String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.RAW_DELTA, name, description, units));
    }


    public MetricTemplate rate(String name, String description, String units, String nom, String div) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.WINDOWED_RATE, name, description, units, nom, div));
    }

    public MetricTemplate util(String name, String description, String units, String nom, String div) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.UTILIZATION, name, description, units, nom, div));
    }

    /**
     * Creates new JMX metrics scanner object. Scanner objects are responsible for scanning
     * selected values accessible via JMX and
     *
     * @param name scanner name
     *
     * @param qdefs queries
     *
     * @return scanner object
     */
    public TraceOutputJmxScanner scanner(String name, QueryDef... qdefs) {
        return new TraceOutputJmxScanner(symbolRegistry, metricsRegistry, name, mbsRegistry, tracer, qdefs);
    }


    public HiccupMeter cpuHiccup(String mbsName, String mbeanName, String attr) {
        return cpuHiccup(mbsName, mbeanName, attr, 10, 30000);
    }


    public HiccupMeter cpuHiccup(String mbsName, String mbeanName, String attr, long resolution, long delay) {
        MethodCallStatistic mcs = mbsRegistry.getOrRegister(mbsName, mbeanName, attr, new MethodCallStatistic("cpuHiccup"));
        HiccupMeter meter = HiccupMeter.cpuMeter(resolution, delay, mcs);
        return meter;
    }


    public HiccupMeter memHiccup(String mbsName, String mbeanName, String attr) {
        return memHiccup(mbsName, mbeanName, attr, 10, 30000);
    }


    public HiccupMeter memHiccup(String mbsName, String mbeanName, String attr, long resolution, long delay) {
        MethodCallStatistic mcs = mbsRegistry.getOrRegister(mbsName, mbeanName, attr, new MethodCallStatistic("memHiccup"));
        HiccupMeter meter = HiccupMeter.memMeter(resolution, delay, mcs);
        return meter;
    }


    public HiccupMeter dskHiccup(String mbsName, String mbeanName, String attr, String path) {
        return dskHiccup(mbsName, mbeanName, attr, 1000, 30000, path);
    }


    public HiccupMeter dskHiccup(String mbsName, String mbeanName, String attr, long resolution, long delay, String path) {
        MethodCallStatistic mcs = mbsRegistry.getOrRegister(mbsName, mbeanName, attr, new MethodCallStatistic("dskHiccup"));
        HiccupMeter meter = HiccupMeter.dskMeter(resolution, delay, path, mcs);
        return meter;
    }

}
