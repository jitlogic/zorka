/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
    public static final int TUNER_CALLS      = 35;
    public static final int TUNER_DROPS      = 36;
    public static final int TUNER_ECALLS     = 37;
    public static final int TUNER_LCALLS     = 38;
    public static final int TUNER_CYCLES     = 39;
    public static final int TUNER_EXCLUSIONS = 40;


    private static final String[] counterNames = {
            "MethodsInstrumented",  //  0
            "SpyErrors",            //  1
            "TracerErrors",         //  2
            "AgentRequests",        //  3
            "AgentErrors",          //  4
            "AgentTime",            //  5
            "TrapsSubmitted",       //  6
            "TrapsSent",            //  7
            "TrapsDropped",         //  8
            "ZabbixRequests",       //  9
            "ZabbixErrors",         // 10
            "ZabbixTime",           // 11
            "NagiosRequests",       // 12
            "NagiosErrors",         // 13
            "NagiosTime",           // 14
            "PerfMonCycles",        // 15
            "PerfMonTime",          // 16
            "PerfPacketsSent",      // 17
            "PerfSamplesSent",      // 18
            "PerfMonWarnings",      // 19
            "PerfMonErrors",        // 20
            "PerfMonNulls",         // 21
            "PerfMonQueries",       // 22
            "TracesSubmitted",      // 23
            "TracesDropped",        // 24
            "MetricsCreated",       // 25
            "AvgCounterErrors",     // 26
            "AvgCountersCreated",   // 27
            "ConfigErrors",         // 28
            "SpySubmissions",       // 29
            "ZorkaStatsCreated",    // 30
            "ZicoPacketsSent",      // 31
            "ZicoPacketsDropped",   // 32
            "ZicoPacketsLost",      // 33
            "ZicoReconnects",       // 34
            "TunerCalls",           // 35
            "TunerDrops",           // 36
            "TunerECalls",          // 37
            "TunerLCalls",          // 38
            "TunerCycles",          // 39
            "TunerExclusions",      // 40
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

    public synchronized static void clear() {
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
