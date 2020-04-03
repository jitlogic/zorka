package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.DTraceContext;
import com.jitlogic.zorka.common.tracedata.PerfTextChunk;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static com.jitlogic.zorka.core.spy.TracerLib.F_SENT;

public class DTraceOutput implements ZorkaSubmitter<SymbolicRecord> {

    private static final Logger log = LoggerFactory.getLogger(DTraceOutput.class);

    private int softLimit = 64 * 1024;
    private int hardLimit = 48 * 1024;

    private DTraceFormatter formatter;
    private ZorkaSubmitter<PerfTextChunk> sender;

    public DTraceOutput(DTraceFormatter formatter, ZorkaSubmitter<PerfTextChunk> sender) {
        this.formatter = formatter;
        this.sender = sender;
    }

    private void process(TraceRecord tr, List<TraceRecord> acc) {
        DTraceContext ds = tr.getDTraceState();
        if (ds != null && !ds.hasFlags(F_SENT)) {
            acc.add(tr); ds.markFlags(F_SENT);
        }
    }

    /** */
    private void submit(List<TraceRecord> acc) {
        while (acc.size() > 0) {
            byte[] buf = formatter.format(acc, softLimit, hardLimit);
            if (log.isTraceEnabled()) {
                String s = buf != null ? new String(buf) : "<null>";
                log.trace("Submitting data to zipkin: " + s);
            }
            if (buf != null) sender.submit(new PerfTextChunk("ZIPKIN-TRACE", buf));
        }
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        if (sr instanceof TraceRecord) {
            List<TraceRecord> acc = new LinkedList<TraceRecord>();
            process((TraceRecord) sr, acc);
            if (log.isTraceEnabled()) log.trace("DTraceOutput: got {} items to send.", acc.size());
            if (acc.size() > 0) submit(acc);
            return true;
        }
        return false;
    }
}
