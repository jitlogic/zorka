package com.jitlogic.zorka.core.spy.st;

import com.jitlogic.zorka.cbor.CborDataReader;
import com.jitlogic.zorka.cbor.SimpleValResolver;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.cbor.CborResendException;
import com.jitlogic.zorka.core.spy.ZicoHttpOutput;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class STraceHttpOutput extends ZicoHttpOutput {

    private SymbolsScanner scanner;

    public static int chunksLength(STraceBufChunk chunks) {
        int len = 0;
        for (STraceBufChunk c  = chunks; c != null; c = c.getNext()) {
            len = Math.max(len, c.getOffset()+c.getSize());
        }
        return len;
    }

    public static byte[] chunksMerge(STraceBufChunk chunks) {
        byte[] buf = new byte[chunksLength(chunks)];

        for (STraceBufChunk c = chunks; c != null; c = c.getNext()) {
            System.arraycopy(c.getBuffer(), 0, buf, c.getOffset(), c.getSize());
        }

        return buf;
    }

    public STraceHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry) {
        super(config, conf, registry);
        scanner = new SymbolsScanner(registry);
    }

    @Override
    protected void resetState() {
        if (log.isDebugEnabled()) {
            log.debug("Resetting state ...");
        }
        scanner.reset();
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
                byte[] b = chunksMerge((STraceBufChunk) sr);
                String trc = DatatypeConverter.printBase64Binary(b);
                try {
                    synchronized (scanner) {
                        scanner.clear();
                        if (log.isTraceEnabled()) {
                            log.trace("OLD data: (pos=" + scanner.getPosition() + "): " + scanner.getData());
                        }
                        if (sessionUUID == null) newSession();
                        new CborDataReader(b, scanner, svr).read();
                        String agd = scanner.getData();
                        if (log.isTraceEnabled()) {
                            log.trace("AGD data: (pos=" + scanner.getPosition() + "): " + agd);
                        }
                        if (agd != null) {
                            send(agd, submitAgentUrl, null);
                        }
                    }

                    if (log.isTraceEnabled()) {
                        log.trace("TRC data: " + trc);
                    }
                    send(trc, submitTraceUrl, UUID.randomUUID().toString());
                    break;
                } catch (CborResendException e) {
                    log.info("Session expired. Reauthenticating ...");
                    newSession();
                } catch (Exception e) {
                    log.error("Error sending trace record: " + e + ". Resetting connection.", e);
                    newSession();
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
