/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.core.ZorkaLib;
import com.jitlogic.zorka.core.integ.*;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.common.tracedata.MetricTemplate;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.List;
import java.util.Map;

public class PerfMonLib {

    /** Reference to symbol registry */
    private SymbolRegistry symbolRegistry;

    /** Reference to metrics registry */
    private MetricsRegistry metricsRegistry;

    private Tracer tracer;

    private ZorkaLib zorkaLib;

    private MBeanServerRegistry mbsRegistry;

    public PerfMonLib(SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, Tracer tracer,
                      MBeanServerRegistry mbsRegistry, ZorkaLib zorkaLib) {
        this.tracer = tracer;
        this.mbsRegistry = mbsRegistry;
        this.symbolRegistry = symbolRegistry;
        this.metricsRegistry = metricsRegistry;
        this.zorkaLib = zorkaLib;
    }


    public MetricTemplate metric(String domain, String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.RAW_DATA, domain, name, description, units));
    }


    public MetricTemplate timedDelta(String domain, String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.TIMED_DELTA, domain, name, description, units));
    }


    public MetricTemplate delta(String domain, String name, String description, String units) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.RAW_DELTA, domain, name, description, units));
    }


    public MetricTemplate rate(String domain, String name, String description, String units, String nom, String div) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.WINDOWED_RATE, domain, name, description, units, nom, div));
    }

    public MetricTemplate util(String domain, String name, String description, String units, String nom, String div) {
        return metricsRegistry.getTemplate(new MetricTemplate(MetricTemplate.UTILIZATION, domain, name, description, units, nom, div));
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
        return HiccupMeter.dskMeter(resolution, delay, path, mcs);
    }


    public PerfAttrFilter attrFilter(Map<String,String> constAttrs, List<String> include, List<String> exclude) {
        return new PerfAttrFilter(symbolRegistry, constAttrs, include, exclude);
    }


    public PerfSampleMatcher sampleMatcher(String pattern) {
        return new PerfSampleMatcher(symbolRegistry, pattern);
    }


    public PerfSampleFilter sampleFilter(Map<String,String> include, Map<String,String> exclude) {
        return new PerfSampleFilter(symbolRegistry, include, exclude);
    }


    public TcpTextOutput tcpTextOutput(String name, Map<String,String> config) {
        TcpTextOutput tcpOutput = new TcpTextOutput(name, config);
        tcpOutput.start();
        return tcpOutput;
    }


    public HttpTextOutput httpTextOutput(String name, Map<String,String> config, Map<String,String> urlParams, Map<String,String> headers) {
        HttpTextOutput httpOutput = new HttpTextOutput(name, config, urlParams, headers);
        httpOutput.start();
        return httpOutput;
    }

    public UrlEndpoint httpTextEndpoint(String uri) {
        return new HttpTextEndpoint(uri);
    }

    public InfluxPushOutput influxPushOutput(
            Map<String,String> config, Map<String,String> constAttrs, PerfAttrFilter attrFilter,
            PerfSampleFilter sampleFilter, ZorkaSubmitter<byte[]> httpOutput) {
        return new InfluxPushOutput(symbolRegistry, config, constAttrs, attrFilter, sampleFilter, httpOutput);
    }


    public OpenTsdbPushOutput tsdbPushOutput(
            Map<String,String> config, Map<String,String> constAttrs, PerfAttrFilter attrFilter,
            PerfSampleFilter sampleFilter, ZorkaSubmitter<byte[]> httpOutput) {
        return new OpenTsdbPushOutput(symbolRegistry, config, constAttrs, attrFilter, sampleFilter, httpOutput);
    }


    public GraphitePushOutput graphitePushOutput(
            Map<String,String> config,
            Map<String,String> constAttrs, PerfAttrFilter attrFilter,
            PerfSampleFilter sampleFilter, ZorkaSubmitter<byte[]> tcpOutput) {
        return new GraphitePushOutput(symbolRegistry, config, constAttrs, attrFilter, sampleFilter, tcpOutput);
    }


    public PrometheusPushOutput prometheusPushOutput(
            Map<String,String> config,
            Map<String,String> constAttrs, PerfAttrFilter attrFilter,
            PerfSampleFilter sampleFilter, ZorkaSubmitter<byte[]> tcpOutput) {
        return new PrometheusPushOutput(symbolRegistry, config, constAttrs, attrFilter, sampleFilter, tcpOutput);
    }

    private ThreadMonitor threadMonitor = null;

    public synchronized ThreadMonitor threadMonitor() {
        if (threadMonitor == null) {
            threadMonitor = new ThreadMonitor(mbsRegistry);
        }
        return threadMonitor;
    }

    public ThreadMonitorDumper threadDumper(ZorkaSubmitter<String> output, int maxThreads, int minCpuThread,
                                            int minCpuTotal, int stackDepth) {
        return new ThreadMonitorDumper(
                mbsRegistry, threadMonitor(), output,
                maxThreads, minCpuThread, minCpuTotal, stackDepth);
    }

    private List<ThreadMonitorRankItem> avgList(int avg) {
        switch (avg) {
            case 1:
                return threadMonitor().getRavg1();
            case 5:
                return threadMonitor().getRavg5();
            case 15:
                return threadMonitor().getRavg15();
            default:
                throw new ZorkaRuntimeException("Invalid average type: " + avg);
        }
    }

    public double threadRankCpu(int avg, int nth) {
        List<ThreadMonitorRankItem> lst = avgList(avg);
        return nth < lst.size() ? lst.get(nth).getAvg() : 0.0;
    }

    public String threadRankName(int avg, int nth) {
        List<ThreadMonitorRankItem> lst = avgList(avg);
        return nth < lst.size() ? lst.get(nth).getName() : "<N/A>";
    }

}
