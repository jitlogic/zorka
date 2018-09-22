/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.tuner;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.KVSortingHeap;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyRetransformer;

import java.util.*;

import static com.jitlogic.zorka.core.AgentConfigProps.*;
import static com.jitlogic.zorka.common.stats.AgentDiagnostics.*;

public class TracerTuner extends ZorkaAsyncThread<TraceTuningStats> {

    private boolean trace;

    private long calls;

    private volatile long lastCalls;

    private int[] ranks;

    /** Interval between tuning cycles. */
    private long interval;

    private int rankSize;

    /** If true, automatic methods exclusion will be enabled. */
    private boolean auto;

    /** Maximum number of methods to exclude in one cycle. */
    private int maxItems;

    /** Approximate percentage of all counted method calls to be excluded in one cycle (when exceeded, exclusions will stop). */
    private int maxRatio;

    /** Minimum number of calls in last cycle that will trigger automatic exclusion of top offender methods. */
    private long minTotalCalls;

    private long minMethodRank;

    /** Last tuning cycle timestamp. */
    private long tstlast = 0L;

    private volatile List<RankItem> rankList = new ArrayList<RankItem>();

    private SymbolRegistry registry;

    private SpyRetransformer retransformer;

    private ZtxMatcherSet tracerMatcherSet;

    private int statCacheMax = 16;

    private Deque<TraceTuningStats> statCache = new LinkedList<TraceTuningStats>();

    public TracerTuner(ZorkaConfig config, SymbolRegistry registry, SpyRetransformer retransformer, ZtxMatcherSet tracerMatcherSet) {
        super("TRACER-TUNER", config.intCfg(TRACER_TUNER_QLEN_PROP, TRACER_TUNER_QLEN_DEFV), 2);

        this.registry = registry;
        this.retransformer = retransformer;
        this.tracerMatcherSet = tracerMatcherSet;

        this.rankSize = config.intCfg(TRACER_TUNER_RANKS_PROP, TRACER_TUNER_RANKS_DEFV);
        this.interval = config.intCfg(TRACER_TUNER_INTERVAL_PROP, TRACER_TUNER_INTERVAL_DEFV) * 1000000L;
        this.auto = config.boolCfg(TRACER_TUNER_AUTO_PROP, TRACER_TUNER_AUTO_DEFV);
        this.minTotalCalls = config.longCfg(TRACER_TUNER_MIN_CALLS_PROP, TRACER_TUNER_MIN_CALLS_DEFV);
        this.minMethodRank = config.longCfg(TRACER_TUNER_MIN_RANK_PROP, TRACER_TUNER_MIN_RANK_DEFV);

        this.maxRatio = config.intCfg(TRACER_TUNER_MAX_RATIO_PROP, TRACER_TUNER_MAX_RATIO_DEFV);
        this.maxItems = config.intCfg(TRACER_TUNER_MAX_ITEMS_PROP, TRACER_TUNER_MAX_ITEMS_DEFV);

        ranks = new int[1024];

        trace = log.isTraceEnabled();

        log.info("Tracer tuner: auto=" + auto + ", interval=" + interval + "ns, rankSize=" + rankSize +
                ", threshold=" + minTotalCalls + "methods/cycle" + ", ratio=" + maxRatio + "pct, mpc=" + maxItems);
    }

    private synchronized void tuningCycle() {

        log.info("Starting tuning cycle (dsize=" + ranks.length + ")");

        lastCalls = calls;

        AgentDiagnostics.inc(TUNER_CYCLES);
        AgentDiagnostics.inc(TUNER_CALLS, calls);

        calcRanks();
        clearStats();


        if (log.isDebugEnabled()) {
            log.debug("Tuner status (before exclusion): " + getStatus());
        }

        log.debug("auto=" + auto + ", lastCalls=" + lastCalls + ", minTotalCalls=" + minTotalCalls);

        if (auto && lastCalls >= minTotalCalls) {
            exclude(maxItems, false);
        }
    }

    public synchronized int exclude(int nitems, boolean force) {

        long lcur = 0;
        long lmax = force ? Long.MAX_VALUE : (lastCalls * 100L / maxRatio);

        log.debug("Looking for classes to exclude: lastCalls=" + lastCalls + ", lmax=" + lmax);

        int rc;

        Set<String> classNames = new HashSet<String>();
        for (rc = 0; rc < Math.min(nitems, rankList.size()); rc++) {
            RankItem ri = rankList.get(rc);
            int mid = ri.getMid();
            int[] cms = registry.methodDef(mid);
            if (cms != null && ri.getRank() > minMethodRank && !tracerMatcherSet.isExcluded(mid)) {
                String className = registry.symbolName(cms[0]);
                String methodName = registry.symbolName(cms[1]);
                String methodSignature = registry.symbolName(cms[2]);
                log.debug("Exclusion: " + className + "|" + methodName + "|" + methodSignature);
                classNames.add(className);
                tracerMatcherSet.add(className, methodName, methodSignature);
            }

            lcur += ri.getRank();
            if (lcur > lmax) break;
        }

        if (rc > 0) {
            rankList = rankList.subList(rc, rankList.size());
            AgentDiagnostics.inc(TUNER_EXCLUSIONS, rc);
        }

        if (!classNames.isEmpty()) {
            log.info("Reinstrumenting classes: " + classNames);

            retransformer.retransform(classNames);
        } else {
            log.debug("No classes to reinstrument.");
        }

        return rc;
    }

    private synchronized void clearStats() {
        calls = 0;
        for (int i = 0; i < ranks.length; i++) {
            ranks[i] = 0;
        }
    }

    private void calcRanks() {
        KVSortingHeap heap = new KVSortingHeap(rankSize, true);

        for (int i = 0; i < ranks.length; i++) {
            int r = ranks[i];
            if (r > 0) heap.add(i, r);
            if (trace) {
                log.trace(i + "| RANK: r=" + r + ": " + registry.methodXDesc(i));
            }
        }

        List<RankItem> rl = new ArrayList<RankItem>();
        for (int i = heap.next(); i > 0; i = heap.next()) {
            int r = ranks[i];
            if (!tracerMatcherSet.isExcluded(i)) {
                rl.add(new RankItem(i, r));
            }
        }

        ZorkaUtil.reverseList(rl);

        this.rankList = rl;
    }

    private synchronized void processDetails(TraceTuningStats detail) {
        if (detail.getSize() > ranks.length) {
            resize(detail.getSize());
        }

        long[] stats = detail.getStats();

        for (int i = 0; i < stats.length; i++) {
            long l = stats[i];
            if (l != 0) {
                int mid = (int) (l & TraceTuningStats.MID_MASK);
                if (mid >= ranks.length) resize(mid);
                ranks[mid] += (l >>> 32);
            }
        }
    }

    private synchronized void resize(int size) {
        int sz = ((size+16384) >>> 14) << 14;
        log.debug("Resizing rank table to " + sz + " items.");
        ranks = ZorkaUtil.clipArray(ranks, sz);
    }

    private synchronized void processStats(TraceTuningStats stats) {

        if (log.isDebugEnabled())
            log.debug("Processing stats: " + stats);

        calls += stats.getCalls();

        processDetails(stats);

        long tstamp = stats.getTstamp();

        if (tstamp - tstlast > interval) {
            if (tstlast != 0) {
                tuningCycle();
            }
            tstlast = tstamp;
        }

        // Return stats struct to reuse
        stats.clear();
        if (statCache.size() < statCacheMax)
            statCache.addFirst(stats);
    }

    @Override
    protected void process(List<TraceTuningStats> obj) {
        for (TraceTuningStats stats : obj) {
            try {
                processStats(stats);
            } catch (Exception e) {
                log.error("Error processing stats: " + stats, e);
            }
        }
    }

    public List<RankItem> getRankList() {
        return rankList;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Status: interval=%dms summary_calls=%d%n", interval/1000000, lastCalls));
        sb.append("Method call ranks (first 32 entries).\n");

        List<RankItem> lst = rankList;

        if (lst != null && !lst.isEmpty()) {
            for (int i = 0; i < Math.min(lst.size(), 32); i++) {
                RankItem itm = lst.get(i);
                sb.append(String.format("%d: R=%d %s%n", i, itm.getRank(), registry.methodXDesc(itm.getMid())));
            }
        } else {
            sb.append("N/A");
        }

        return sb.toString();
    }

    public TraceTuningStats exchange(TraceTuningStats stats) {

        if (stats != null) submit(stats);

        TraceTuningStats s  = statCache.pollLast();

        return s != null ? s : new TraceTuningStats();
    }

    public long getLastCalls() {
        return lastCalls;
    }
}
