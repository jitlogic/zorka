package com.jitlogic.zorka.core.spy.st;

import com.jitlogic.zorka.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.spy.ZicoHttpOutput;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class STraceHttpOutput extends ZicoHttpOutput {

    private final SymbolsScanner scanner;

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
                STraceBufChunk chunk = (STraceBufChunk) sr;
                Base64FormattingStream bs = null;
                try {
                    synchronized (scanner) {
                        scanner.clear();
                        if (sessionUUID == null) newSession();
                        new CborDataReader(new ChunkedCborInput(chunk), scanner, svr).read();
                        if (scanner.getPosition() > 0) {
                            bs = new Base64FormattingStream(new ByteArrayCborInput(scanner.getBuf(), 0, scanner.getPosition()));
                            send(bs, bs.available(), submitAgentUrl, null);
                        }
                    }

                    bs = new Base64FormattingStream(new ChunkedCborInput(chunk));
                    send(bs, bs.available(), submitTraceUrl, UUID.randomUUID().toString());
                    break;
                } catch (CborResendException e) {
                    log.info("Session expired. Reauthenticating ...");
                    newSession();
                } catch (Exception e) {
                    log.error("Error sending trace record: " + e + ". Resetting connection.", e);
                    newSession();
                } finally {
                    if (bs != null) {
                        try {
                            bs.close();
                        } catch (Exception e) {
                            log.warn("Cannot close BIS:", e);
                        }
                    }
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
