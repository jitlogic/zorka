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

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;

import java.util.List;

/**
 * JMX Attribute Scanner is responsible for traversing JMX (using supplied queries) and
 * submitting obtained metric data to tracer output. Results will be sorted into categories
 * (integers, long integers, double precision FP), attribute sets will be concatenated and
 * converted to symbols, data of each category will be submitted.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceOutputJmxScanner extends JmxScanner implements Runnable {

    /**
     * Output handler - handles generated data (eg. saves them to trace files).
     */
    private ZorkaSubmitter<SymbolicRecord> output;


    /**
     * Scanner name.
     */
    protected String name;


    /**
     * Scanner ID (attached to every packet of sample data).
     */
    protected int id;


    /**
     * Creates new JMX attribute scanner object.
     *
     * @param symbols  symbol registry
     * @param name     scanner name (converted to ID using symbol registry and attached to every emitted packet of data).
     * @param registry MBean server registry object
     * @param output   tracer output
     * @param qdefs    JMX queries
     */
    public TraceOutputJmxScanner(SymbolRegistry symbols, MetricsRegistry metricRegistry, String name,
                                 MBeanServerRegistry registry, ZorkaSubmitter<SymbolicRecord> output, QueryDef... qdefs) {

        super(registry, metricRegistry, symbols, qdefs);

        this.output = output;
        this.name = name;
        this.id = symbols.symbolId(name);
    }


    @Override
    public void run() {
        try {
            runCycle(System.currentTimeMillis());
        } catch (Error e) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Error executing scanner '" + name + "'", e);
            AgentDiagnostics.inc(AgentDiagnostics.PMON_ERRORS);
        }
    }


    /**
     * Performs one scan-submit cycle.
     *
     * @param clock current time (milliseconds since Epoch)
     */
    public void runCycle(long clock) {
        AgentDiagnostics.inc(AgentDiagnostics.PMON_CYCLES);

        long t1 = System.nanoTime();

        List<PerfSample> samples = getPerfSamples(clock);

        long t2 = System.nanoTime();

        log.info(ZorkaLogger.ZPM_RUNS, "Scanner %s execution took " + (t2 - t1) / 1000000L + " milliseconds to execute. "
                + "Collected samples: " + samples.size());

        AgentDiagnostics.inc(AgentDiagnostics.PMON_TIME, t2 - t1);
        AgentDiagnostics.inc(AgentDiagnostics.PMON_PACKETS_SENT);
        AgentDiagnostics.inc(AgentDiagnostics.PMON_SAMPLES_SENT, samples.size());

        if (samples.size() > 0) {
            output.submit(new PerfRecord(clock, id, samples));
        }
    }


}
