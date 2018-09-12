package com.jitlogic.zorka.core.spy.tuner;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.KVSortingHeap;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyRetransformer;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.*;

import static com.jitlogic.zorka.core.spy.tuner.TraceDetailStats.*;

public class TracerTuner extends ZorkaAsyncThread<TraceSummaryStats> {

    private long calls;
    private long drops;
    private long errors;
    private long lcalls;

    private volatile long lastCalls;
    private volatile long lastDrops;
    private volatile long lastErrors;
    private volatile long lastLCalls;

    private long[] callsv;
    private long[] dropsv;
    private int[] errorsv;
    private int[] lcallsv;

    private int dsize = 1024;

    /** Interval between tuning cycles. */
    private long interval;

    private int rankSize;

    /** If true, automatic methods exclusion will be enabled. */
    private boolean auto;

    /** Maximum number of methods to exclude in one cycle. */
    private int autoMpc;

    /** Approximate percentage of all counted method calls to be excluded in one cycle (when exceeded, exclusions will stop). */
    private int autoRatio;

    /** Minimum number of calls in last cycle that will trigger automatic exclusion of top offender methods. */
    private long autoCalls;

    /** Last tuning cycle timestamp. */
    private long tstlast = 0L;

    private volatile List<RankItem> rankList = new ArrayList<RankItem>();

    private SymbolRegistry registry;
    private SpyRetransformer retransformer;

    private int statCacheMax = 16;
    private Deque<TraceSummaryStats> statCache = new LinkedList<TraceSummaryStats>();

    public TracerTuner(ZorkaConfig config, SymbolRegistry registry, SpyRetransformer retransformer) {
        super("TRACER-TUNER", 1024, 1);

        this.registry = registry;
        this.retransformer = retransformer;

        this.rankSize = config.intCfg("tracer.tuner.ranks", 31);
        this.interval = config.intCfg("tracer.tuner.interval", 30000) * 1000000L;
        this.auto = config.boolCfg("tracer.tuner.auto", false);
        this.autoCalls = config.longCfg("tracer.tuner.auto.calls", 100L) * 1000000;
        this.autoRatio = config.intCfg("tracer.tuner.auto.ratio", 50);
        this.autoMpc = config.intCfg("tracer.tuner.auto.mpc", 10);

        callsv = new long[dsize];
        dropsv = new long[dsize];
        errorsv = new int[dsize];
        lcallsv = new int[dsize];
    }

    private synchronized void tuningCycle() {
        lastCalls = calls;
        lastDrops = drops;
        lastErrors = errors;
        lastLCalls = lcalls;

        calcRanks();
        clearStats();

        if (auto && lastCalls >= autoCalls) {
            exclude(autoMpc);
        }
    }

    public synchronized void exclude(int nitems) {
        long cmax = lastCalls * 100L / autoRatio;
        long lcur = 0;
        Set<String> classNames = new HashSet<String>();
        for (int i = 0; i < Math.min(nitems, rankList.size()); i++) {
            RankItem ri = rankList.get(i);
            int mid = ri.getMid();
            int[] cms = registry.methodDef(mid);
            if (cms != null) {
                String className = registry.symbolName(cms[0]);
                if (className != null) {
                    classNames.add(className);
                }
            }

            lcur += ri.getCalls();
            if (lcur > cmax) break;
        }

        if (!classNames.isEmpty()) {
            log.info("Excluding classes: " + classNames);
            retransformer.retransform(classNames);
        }

    }

    private synchronized void clearStats() {
        calls = drops = errors = lcalls = 0;
        for (int i = 0; i < dsize; i++) {
            callsv[i] = 0;
            dropsv[i] = 0;
            errorsv[i] = 0;
            lcallsv[i] = 0;
        }
    }

    private void calcRanks() {
        KVSortingHeap heap = new KVSortingHeap(rankSize, true);

        for (int i = 0; i < dsize; i++) {
            int r = rank(i);
            if (r > 0) heap.add(i, r);
        }

        List<RankItem> rl = new ArrayList<RankItem>();
        for (int i = heap.next(); i > 0; i = heap.next()) {
            int r = rank(i);
            rl.add(new RankItem(i, r, callsv[i], dropsv[i]));
        }
        this.rankList = rl;
    }

    private int rank(int mid) {
        if (callsv[mid] == 0) return -1;
        long c = callsv[mid];
        long d = dropsv[mid];
        return (int) Math.min(c-((c-d)>>>2)-(errorsv[mid]<<4)-(lcallsv[mid]<<8), Integer.MAX_VALUE);
    }

    private synchronized void processDetails(TraceDetailStats detail) {
        if (detail.getSize() > dsize) {
            resize(detail.getSize());
        }

        long[] sws = detail.getStats();

        for (int i = 0; i < detail.getSize(); i++) {
            long l = sws[i];
            callsv[i] += l & CALL_MASK;
            dropsv[i] += (l & DROP_MASK) >>> DROP_BITS;
            errorsv[i] += (l & ERR_MASK) >>> ERR_BITS;
            lcallsv[i] += (l & LONG_MASK) >>> LONG_BITS;
        }
    }

    private synchronized void resize(int size) {
        int sz = ((size+1023) >>> 10) << 10;

        callsv = ZorkaUtil.clipArray(callsv, sz);
        dropsv = ZorkaUtil.clipArray(dropsv, sz);
        errorsv = ZorkaUtil.clipArray(errorsv, sz);
        lcallsv = ZorkaUtil.clipArray(lcallsv, sz);
    }

    private synchronized void processStats(TraceSummaryStats ss, long tstamp) {
        calls += ss.getCalls();
        drops += ss.getDrops();
        errors += ss.getErrors();
        lcalls += ss.getLcalls();

        if (ss.getDetails() != null) {
            processDetails(ss.getDetails());
        }

        if (tstamp - tstlast > interval) {
            if (tstlast != 0) {
                tuningCycle();
            }
            tstlast = tstamp;
        }

        ss.clear();
        switch (Tracer.getTuningMode()) {
            case Tracer.TUNING_SUM:
                ss.setDetails(null);
                if (statCache.size() < statCacheMax)
                    statCache.addFirst(ss);
                break;
            case Tracer.TUNING_DET:
                if (statCache.size() < statCacheMax &&
                        ss.getDetails() != null &&
                        ss.getDetails().getSize() == dsize)
                    statCache.addFirst(ss);
                break;
            default:
                break;
        }
    }

    @Override
    protected void process(List<TraceSummaryStats> obj) {
        for (TraceSummaryStats stats : obj) {
            processStats(stats, System.nanoTime());
        }
    }

    public List<RankItem> getRankList() {
        return rankList;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Status: interval=%dms%n", interval/1000000));
        sb.append(String.format("calls=%d, drops=%d, errs=%d, lcalls=%d%n", lastCalls, lastDrops, lastErrors, lastLCalls));


        return sb.toString();
    }

    public TraceSummaryStats exchange(TraceSummaryStats stats) {
        submit(stats);

        for (TraceSummaryStats s  = statCache.pollLast(); s != null; s = statCache.pollLast()) {
            if (Tracer.getTuningMode() == Tracer.TUNING_SUM) {
                s.setDetails(null);
                return s;
            }
            if (Tracer.getTuningMode() == Tracer.TUNING_DET && s.getDetails() != null && s.getDetails().getSize() == dsize) {
                return s;
            }
        }

        TraceSummaryStats s = new TraceSummaryStats();
        if (Tracer.getTuningMode() == Tracer.TUNING_DET) {
            s.setDetails(new TraceDetailStats(dsize));
        }
        return s;
    }
}
