package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.spy.stracer.ChunkedCborInput;
import com.jitlogic.zorka.core.spy.stracer.STraceBufChunk;
import com.jitlogic.zorka.core.spy.stracer.SymbolsScanner;

import java.util.List;
import java.util.Map;


public class STraceHttpOutput extends ZicoHttpOutput {

    private final SymbolsScanner scanner;

    public static int chunksLength(STraceBufChunk chunks) {
        int len = 0;
        for (STraceBufChunk c  = chunks; c != null; c = c.getNext()) {
            len = Math.max(len, c.getExtOffset()+c.getPosition());
        }
        return len;
    }

    public static byte[] chunksMerge(STraceBufChunk chunks) {
        byte[] buf = new byte[chunksLength(chunks)];

        for (STraceBufChunk c = chunks; c != null; c = c.getNext()) {
            System.arraycopy(c.getBuffer(), 0, buf, c.getExtOffset(), c.getPosition());
        }

        return buf;
    }

    public STraceHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry) {
        super(config, conf, registry);
        scanner = new SymbolsScanner(registry);
    }

    private void resetState() {
        if (log.isDebugEnabled()) {
            log.debug("Resetting state ...");
        }
        scanner.reset();
        isClean = true;
    }

    private static SimpleValResolver svr = new SimpleValResolver() {
        @Override
        public Object resolve(int sv) {
            return null;
        }
    };

    @Override
    protected void process(List<SymbolicRecord> obj) {
        for (SymbolicRecord sr : obj) {
            long rt = retryTime;
            for (int i = 0; i < retries+1; i++) {
                STraceBufChunk chunk = (STraceBufChunk) sr;
                try {
                    synchronized (scanner) {
                        scanner.clear();
                        CborInput input = new ChunkedCborInput(chunk);
                        CborDataReader rdr = new CborDataReader(input, scanner, svr);
                        while (!input.eof()) rdr.read();
                        if (scanner.getPosition() > 0) {
                            send(scanner.getBuf(), scanner.getPosition(), submitAgentUrl, 0L, 0L, isClean);
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
