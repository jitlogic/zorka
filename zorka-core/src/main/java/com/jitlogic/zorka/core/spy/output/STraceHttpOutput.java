package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.http.HttpClient;
import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistrySenderVisitor;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.spy.stracer.ChunkedCborInput;
import com.jitlogic.zorka.core.spy.stracer.STraceBufChunk;

import java.util.List;
import java.util.Map;


public class STraceHttpOutput extends ZicoHttpOutput {

    private TraceDataScanner scanner;
    private SymbolRegistrySenderVisitor visitor;
    private CborDataWriter writer;

    public static int chunksLength(STraceBufChunk chunks) {
        int len = 0;
        for (STraceBufChunk c  = chunks; c != null; c = c.getNext()) {
            len = Math.max(len, c.getExtOffset()+c.getPosition());
        }
        return len;
    }


    public STraceHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry, HttpHandler httpClient) {
        super(config, conf, registry, httpClient);
        writer = new CborDataWriter(65536, 65536);
        visitor = new SymbolRegistrySenderVisitor(registry, new TraceDataWriter(writer));
        scanner = new TraceDataScanner(visitor, null);
    }


    private void resetState() {
        if (log.isDebugEnabled()) {
            log.debug("Resetting state ...");
        }
        visitor.reset();
        writer.reset();
        isClean = true;
    }


    @Override
    protected void process(List<SymbolicRecord> obj) {
        for (SymbolicRecord sr : obj) {
            long rt = retryTime;
            for (int i = 0; i < retries+1; i++) {
                STraceBufChunk chunk = (STraceBufChunk) sr;
                try {
                    synchronized (scanner) {
                        CborInput input = new ChunkedCborInput(chunk);
                        new TraceDataReader(new CborDataReader(input), new TraceDataScanner(visitor, null)).run();
                        if (writer.position() > 0) {
                            send(writer.getBuf(), writer.position(), submitAgentUrl, 0L, 0L, isClean);
                            writer.reset();
                            isClean = false;
                        }
                    }

                    send(chunk.getBuffer(), chunk.getPosition(), submitTraceUrl, chunk.getTraceId2(), chunk.getTraceId1(), false);
                    break;
                } catch (CborResendException e) {
                    log.info("Session expired. Reauthenticating ...");
                    resetState();
                } catch (Exception e) {
                    log.error("Error sending trace record: " + e + ". Resetting connection.", e);
                    resetState();
                }

                try {
                    log.debug("Will retry (wait=" + rt + ")");
                    Thread.sleep(rt);
                } catch (InterruptedException e) {
                    log.warn("Huh? Sleep interrupted?");
                }

                rt *= retryTimeExp;
            }
        }
    }


}
