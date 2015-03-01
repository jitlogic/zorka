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

package com.jitlogic.zorka.common.stats;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class AgentDiagnostics {

    public static final int METHODS_INSTRUMENTED = 0;
    public static final int SPY_ERRORS = 1;
    public static final int TRACER_ERRORS = 2;
    public static final int AGENT_REQUESTS = 3;
    public static final int AGENT_ERRORS = 4;
    public static final int AGENT_TIME = 5;
    public static final int TRAPS_SUBMITTED = 6;
    public static final int TRAPS_SENT = 7;
    public static final int TRAPS_DROPPED = 8;
    public static final int ZABBIX_REQUESTS = 9;
    public static final int ZABBIX_ERRORS = 10;
    public static final int ZABBIX_TIME = 11;
    public static final int NAGIOS_REQUESTS = 12;
    public static final int NAGIOS_ERRORS = 13;
    public static final int NAGIOS_TIME = 14;
    public static final int PMON_CYCLES = 15;
    public static final int PMON_TIME = 16;
    public static final int PMON_PACKETS_SENT = 17;
    public static final int PMON_SAMPLES_SENT = 18;
    public static final int PMON_WARNINGS = 19;
    public static final int PMON_ERRORS = 20;
    public static final int PMON_NULLS = 21;
    public static final int PMON_QUERIES = 22;
    public static final int TRACES_SUBMITTED = 23;
    public static final int TRACES_DROPPED = 24;
    public static final int METRICS_CREATED = 25;
    public static final int AVG_CNT_ERRORS = 26;
    public static final int AVG_CNT_CREATED = 27;
    public static final int CONFIG_ERRORS = 28;
    public static final int SPY_SUBMISSIONS = 29;
    public static final int ZORKA_STATS_CREATED = 30;
    public static final int ZICO_PACKETS_SENT = 31;
    public static final int ZICO_PACKETS_DROPPED = 32;  // Packets dropped due to queue overflow
    public static final int ZICO_PACKETS_LOST = 33;     // Packets lost due to communication errors
    public static final int ZICO_RECONNECTS = 34;       // ZICO reconnects


    private static final String[] counterNames = {
            "MethodsInstrumented",  // METHODS_INSTRUMENTED = 1
            "SpyErrors",            // SPY_ERRORS           = 2
            "TracerErrors",         // TRACER_ERRORS        = 3
            "AgentRequests",        // AGENT_REQUESTS       = 4
            "AgentErrors",          // AGENT_ERRORS         = 5
            "AgentTime",            // AGENT_TIME           = 6
            "TrapsSubmitted",       // TRAPS_SUBMITTED      = 7
            "TrapsSent",            // TRAPS_SENT           = 8
            "TrapsDropped",         // TRAPS_DROPPED        = 9
            "ZabbixRequests",       // ZABBIX_REQUESTS      = 10
            "ZabbixErrors",         // ZABBIX_ERRORS        = 11
            "ZabbixTime",           // ZABBIX_TIME          = 12
            "NagiosRequests",       // NAGIOS_REQUESTS      = 13
            "NagiosErrors",         // NAGIOS_ERRORS        = 14
            "NagiosTime",           // NAGIOS_TIME          = 15
            "PerfMonCycles",        // PMON_CYCLES          = 16
            "PerfMonTime",          // PMON_TIME            = 17
            "PerfPacketsSent",      // PMON_PACKETS_SENT    = 18
            "PerfSamplesSent",      // PMON_SAMPLES_SENT    = 19
            "PerfMonWarnings",      // PMON_WARNINGS        = 20
            "PerfMonErrors",        // PMON_ERRORS          = 21
            "PerfMonNulls",         // PMON_NULLS           = 22
            "PerfMonQueries",       // PMON_QUERIES         = 23
            "TracesSubmitted",      // TRACES_SUBMITTED     = 24
            "TracesDropped",        // TRACES_DROPPED       = 25
            "MetricsCreated",       // METRICS_CREATED      = 26
            "AvgCounterErrors",     // AVG_CNT_ERRORS       = 27
            "AvgCountersCreated",   // AVG_CNT_CREATED      = 28
            "ConfigErrors",         // CONFIG_ERRORS        = 29
            "SpySubmissions",       // SPY_SUBMISSIONS      = 30
            "ZorkaStatsCreated",    // ZORKA_STATS_CREATED  = 31
            "ZicoPacketsSent",      // ZICO_PACKETS_SENT    = 32
            "ZicoPacketsDropped",   // ZICO_PACKETS_DROPPED = 33
            "ZicoPacketsLost",      // ZICO_PACKETS_LOST    = 34
            "ZicoReconnects",       // ZICO_RECONNECTS      = 35;
    };


    private static Set<Integer> timeCounters = ZorkaUtil.set(AGENT_TIME, ZABBIX_TIME, NAGIOS_TIME, PMON_TIME);


    private static AtomicLong[] counters;


    public static void inc(int counter) {
        counters[counter].incrementAndGet();
    }

    public static void inc(boolean cond, int counter) {
        if (cond) {
            counters[counter].incrementAndGet();
        }
    }


    public static void inc(int counter, long delta) {
        counters[counter].addAndGet(delta);
    }


    public static long get(int counter) {
        long v = (counter < counters.length && counter >= 0) ? counters[counter].get() : 0L;
        return timeCounters.contains(counter) ? v / 1000000L : v;
    }

    public static void clear(int counter) {
        counters[counter].set(0);
    }

    public static void clear() {
        counters = new AtomicLong[counterNames.length];

        for (int i = 0; i < counterNames.length; i++) {
            counters[i] = new AtomicLong(0);
        }
    }

    public static int numCounters() {
        return counterNames.length;
    }

    public static String getName(int counter) {
        return counterNames[counter];
    }

    static {
        clear();
    }
}
