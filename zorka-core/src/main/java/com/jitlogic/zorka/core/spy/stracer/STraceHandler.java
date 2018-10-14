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

package com.jitlogic.zorka.core.spy.stracer;

import com.jitlogic.zorka.cbor.TraceRecordFlags;
import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.cbor.CBOR;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.TracerLib;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.jitlogic.zorka.cbor.TraceDataTags.*;
import static com.jitlogic.zorka.core.util.ZorkaUnsafe.*;

/**
 * Efficient implementation of trace handler that produces CBOR data stream.
 * See ZICO documentation for detailed data format description.
 *
 * This implementation is mostly GC-free and tries to incur minimum performance penalty.
 *
 * @author Rafal Lewczuk
 */
public class STraceHandler extends TraceHandler {

    public static final int STACK_DEFAULT_SIZE = 384;

    public static final int TICK_SHIFT = 16;
    public static final int BPOS_SHIFT = 32;

    public static final int TRACEID_BITS = 16;
    public static final long TRACEID_MASK = 0x00FFFF0000000000L;

    public static final int  TSTAMP_BITS = 40;
    public static final long TSTAMP_MASK = 0x000000FFFFFFFFFFL;

    public static final int  CALLS_BITS = 32;
    public static final long CALLS_MASK = 0x00000000FFFFFFFFL;

    public static final int  TPOS_BITS  = 32;
    public static final long TPOS_MASK  = 0xFFFFFFFF00000000L;

    public static final long TF_BITS = 56;
    public static final long TF_MASK = 0xff00000000000000L;

    public static final long TF_SUBMIT_TRACE  = ((long)TraceRecordFlags.TF_SUBMIT_TRACE) << TF_BITS;
    public static final long TF_SUBMIT_METHOD = ((long)TraceRecordFlags.TF_SUBMIT_METHOD) << TF_BITS;
    public static final long TF_DROP_TRACE    = ((long)TraceRecordFlags.TF_DROP_TRACE) << TF_BITS;
    public static final long TF_ERROR_MARK    = ((long)TraceRecordFlags.TF_ERROR_MARK) << TF_BITS;


    public static final long TSTART_FUZZ = System.currentTimeMillis() - (System.nanoTime() / 1000000L);

    /**
     * This is pre-computed 4-byte trace record header.
     */
    public static final int TREC_HEADER;
    public static final int TREC_EPILOG;

    /**
     * Initial fragment of trace record header (four bytes):
     * - 0xd8 - TRACE_START tag
     * - 0x9f - VCODE - unbounded array start
     * - 0xda/0xdb - PROLOG_BE/PROLOG_LE
     * - 0x48 - prolog start (byte array of length 8)
     */
    public static final int TREC_HEADER_BE = 0xc89fca48;
    public static final int TREC_HEADER_LE = 0x48cb9fc8;

    private final long minMethodTime;   // Default minimum duration = 4 ticks = ~250us
    private long minTraceTime = 16384; // Approximately 1s

    /**
     * If true, streaming mode will be enabled. In streaming mode tracer
     * will be able to send partial data. In non-streaming mode data will
     * be sent only at the end of top-level trace.
     */
    private final boolean streamingEnabled;

    protected STraceBufManager bufManager;
    protected STraceBufChunk chunk = null;
    /** Currently selected output buffer. */
    protected byte[] buffer;
    /** Buffer position and buffer length. */
    protected int bufOffs;
    protected int bufPos;
    protected int bufLen;

    protected int lastPos = 0;

    protected int nchunks = 0;

    protected int minChunks = 2, maxChunks = 16;

    /** Trace buffer output */
    protected ZorkaSubmitter<SymbolicRecord> output;

    protected SymbolRegistry symbols;

    private final boolean debugEnabled;
    private final boolean traceEnabled;

    private long uuidL, uuidH;

    /**
     * Contains 'image' of traced thread call stack containing basic information needed to manage tracing process.
     *
     * Each stack frame is represented by 3 entries:
     * W0: [tstamp|tid|flags]
     * W1: [calls|bufpos]
     * W2: [mid|---]
     *
     * Where:
     * tstamp [40 bit] - timestamp (ticks, 65536ns each)
     * tid    [16 bit] - trace ID  (only for frames marking trace beginning)
     * flags  [8 bit]  - additional flags: TF_ERROR [1], TF_SUBMIT_TRACE[2], TF_SPLIT [4]
     * calls  [32 bit] - counts (encountered) calls;
     * bufpos [32 bit]  - start position in output buffer (used in retracting unneeded records)
     */
    private long[] stack = new long[STACK_DEFAULT_SIZE];

    private final static int W0_OFF = 3;
    private final static int W1_OFF = 2;
    private final static int W2_OFF = 1;

    /** Position of first unused slot in stack[]. If non-zero, Entry0 is at [stackPos-2], Entry1 is at [stackPos-1]. */
    private int stackPos = 0;

    /** Number of traces marked on stack (increases with each recursive  */
    private int tracePos = 0;

    /** Stack length is kept separately, so we avoid indirection when refering to it. */
    private int stackLen = STACK_DEFAULT_SIZE;

    /** Identity hash code of last seen exception. */
    private int exceptionId;

    /** Used to generate UUIDs of traces. */
    private Random random = new Random();

    public STraceHandler(boolean streamingEnabled, STraceBufManager bufManager,
                         SymbolRegistry symbols, TracerTuner tracerTuner, ZorkaSubmitter<SymbolicRecord> output) {

        long mmt = TraceHandler.minMethodTime >>> 16;

        this.streamingEnabled = streamingEnabled;
        this.minMethodTime = mmt;

        this.bufManager = bufManager;
        this.tuner = tracerTuner;
        this.symbols = symbols;
        this.output = output;

        this.debugEnabled = log.isDebugEnabled();
        this.traceEnabled = log.isTraceEnabled();
    }


    public void traceEnter(int methodId, long tstamp) {
        if (disabled) return;

        disabled = true;

        if (stackPos == 1 && 0 == (stack[0] >> TSTAMP_BITS)) {
            bufPos = 0;
        } else if (stackLen - stackPos < 3) {
            extendStack();
        }

        if (bufLen - bufPos < 12) {
            nextChunk();
        }

        long tst = tstamp >>> 16;
        long tr0 = tst | ((long)methodId << TSTAMP_BITS);

        lastPos = bufPos;
        UNSAFE.putInt(buffer, BYTE_ARRAY_OFFS+bufPos, TREC_HEADER);
        UNSAFE.putLong(buffer, BYTE_ARRAY_OFFS+bufPos+4, tr0);

        stack[stackPos] = tst;
        stack[stackPos+1] = 1 + ((long)(bufPos + bufOffs) << 32);
        stack[stackPos+2] = methodId;

        bufPos += 12;
        stackPos += 3;

        disabled = false;
    }

    private final static int MID_MASK = 0x00ffffff;

    public void traceReturn(long tstamp) {
        if (disabled) return;
        if (stackPos == 0) return;

        disabled = true;

        long tst = tstamp >>> 16;

        long w2 = stack[stackPos-W2_OFF];
        long w1 = stack[stackPos-W1_OFF];
        long w0 = stack[stackPos-W0_OFF];

        long dur = tst - (w0 & TSTAMP_MASK);
        int tid = (int)((w0 & TRACEID_MASK) >> TSTAMP_BITS);
        int pos = (int)(w1 >> TPOS_BITS);
        long calls = w1 & CALLS_MASK;

        boolean fsm = 0 != (w0 & TF_SUBMIT_METHOD);

        if (tuningEnabled) {
            tuningProbe((int)(w2 & MID_MASK), tstamp, dur << 16);
        }

        if (dur >= minMethodTime || fsm || pos < bufOffs || tid != 0) {

            // Output trace flags (if any)
            int flags = (int)(w1 >>> TF_BITS);
            if (flags != 0) {
                if (bufLen - bufPos < 2) nextChunk();
                buffer[bufPos] = (byte)(CBOR.TAG_BASE+TAG_TRACE_FLAGS);
                buffer[bufPos+1] = (byte)flags;
                bufPos += 2;
            }

            // Output epilog
            if (calls < 0x1000000) {
                if (bufLen - bufPos < 12) nextChunk();
                writeUInt(CBOR.TAG_BASE, TREC_EPILOG);
                tst |= (calls << TSTAMP_BITS);
                buffer[bufPos]   = (byte) (CBOR.BYTES_BASE+8);
                UNSAFE.putLong(buffer, BYTE_ARRAY_OFFS+bufPos+1, tst);
                buffer[bufPos+9] = (byte) CBOR.BREAK_CODE;
                bufPos += 10;
            } else {
                if (bufLen - bufPos < 20) nextChunk();
                writeUInt(CBOR.TAG_BASE, TREC_EPILOG);
                buffer[bufPos]   = (byte) CBOR.BYTES_BASE+16;
                long p = BYTE_ARRAY_OFFS+bufPos+1;
                UNSAFE.putLong(buffer, p, tst);
                UNSAFE.putLong(buffer, p+8, calls);
                buffer[bufPos+17] = (byte) CBOR.BREAK_CODE;
                bufPos += 18;
            }
        } else {
            bufPos = pos - bufOffs;
        }

        stackPos -= 3;

        if (tid != 0) {

            tracePos--;

            boolean forceSubmit = 0 != (w0 & TF_SUBMIT_TRACE);

            if (traceEnabled) {
                log.trace("TraceReturn: streamingEnabled=" + streamingEnabled + ", tracePos=" + tracePos
                        + ", stackPos=" + stackPos + ", dur=" + dur + ", minTraceTime=" + minTraceTime
                        + ", submitTrace=" + forceSubmit);
            }

            if ((streamingEnabled || tracePos == 0) && (dur >= minTraceTime || forceSubmit)) {
                flush();
            }

            // Reset UUID
            if (tracePos == 0) {
                uuidL = uuidH = 0;
            }
        }

        if (stackPos > 0) {
            long spp = stack[stackPos-W1_OFF];
            stack[stackPos-W1_OFF] =  ((spp & CALLS_MASK) + calls) | (spp & TPOS_MASK);
            if (fsm) {
                stack[stackPos-W0_OFF] |= TF_SUBMIT_METHOD;
            }
        } else  {
            if (bufOffs == 0) {
                bufPos = 0;
            } else {
                dropTrace();
            }
        }

        disabled = false;
    }


    public void serializeException(Throwable e) {
        if (bufLen - bufPos < 3) nextChunk();

        int id = System.identityHashCode(e);

        if (id == exceptionId) {
            // Known (already written) exception - save reference to it;
            writeUInt(CBOR.TAG_BASE, TAG_EXCEPTION_REF);
            writeUInt(CBOR.UINT_BASE, id);
        } else {
            // Unknown exception
            buffer[bufPos] = (byte) (CBOR.TAG_CODE1);
            buffer[bufPos + 1] = (byte) TAG_EXCEPTION;
            buffer[bufPos + 2] = (byte) (CBOR.ARR_CODE0 + 5);
            bufPos += 3;

            // Object identity (useful for determining
            writeInt(id);

            // Class name (as string ref)
            if (bufLen - bufPos < 1) nextChunk();
            buffer[bufPos++] = (byte) (CBOR.TAG_CODE0 + TAG_STRING_REF);
            writeInt(symbols.symbolId(e.getClass().getName()));

            // Message
            writeString(e.getMessage() != null ? e.getMessage() : "");

            writeInt(0); // TODO generate proper causeId
            //Throwable cause = e.getCause();
            //if (cause != null) {
            //    serializeException(cause);
            //} else {
            //    write(CBOR.NULL_CODE);
            //}

            // Stack trace
            StackTraceElement[] stk = e.getStackTrace();
            writeUInt(CBOR.ARR_BASE, stk.length);
            for (StackTraceElement el : stk) {
                writeUInt(CBOR.ARR_BASE, 4);
                writeInt(symbols.symbolId(el.getClassName()));
                writeInt(symbols.symbolId(el.getMethodName()));
                writeInt(symbols.symbolId(el.getFileName()));
                writeInt(el.getLineNumber() >= 0 ? el.getLineNumber() : 0);
            }

        }
    }


    public void traceError(Object e, long tstamp) {
        if (disabled) return;
        if (stackPos == 0) return;

        serializeException((Throwable)e);
        exceptionId = System.identityHashCode(e);

        int mid = (int)(stack[stackPos-W2_OFF] & MID_MASK);

        stack[stackPos-W0_OFF] |= (TF_SUBMIT_METHOD|TF_ERROR_MARK);

        traceReturn(tstamp);

        if (tuningEnabled) {
            tunStats.markRank(mid, ERROR_PENALTY);
        }
    }

    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        if (stackPos == 0) return;

        long w0 = stack[stackPos-W0_OFF];
        stack[stackPos-W0_OFF] = (w0 & TSTAMP_MASK) | ((long)traceId << TSTAMP_BITS);

        writeUInt(CBOR.TAG_BASE, TAG_TRACE_BEGIN);
        writeUInt(CBOR.ARR_BASE, 2);
        writeLong(clock);
        writeInt(traceId);

        if (tracePos == 0) {
            uuidL = random.nextLong();
            uuidH = random.nextLong();
            if (chunk != null) {
                chunk.setUuidL(uuidL);
                chunk.setUuidH(uuidH);
                chunk.setStartOffset(lastPos);
            }
        }

        tracePos++;
    }

    @Override
    public void newAttr(int traceId, int attrId, Object attrVal) {
        if (traceId >= 0) {
            writeUInt(CBOR.TAG_BASE, TAG_TRACE_UP_ATTR);
            if (bufLen - bufPos < 2) nextChunk();
            buffer[bufPos++] = (byte) (CBOR.ARR_BASE+2);
            writeInt(traceId);
        } else {
            writeUInt(CBOR.TAG_BASE, TAG_TRACE_ATTR);
        }
        if (bufLen - bufPos < 2) nextChunk();
        buffer[bufPos++] = (byte) (CBOR.MAP_BASE+1);
        buffer[bufPos++] = (byte)(CBOR.TAG_BASE + TAG_STRING_REF);
        stack[stackPos-W0_OFF] |= TF_SUBMIT_METHOD; // TODO submit force behavior is controlled by API, make this thing configurable as in LTracer

        writeUInt(0,attrId);
        writeObject(attrVal);
    }

    @Override
    public void setMinimumTraceTime(long minTraceTime) {
        this.minTraceTime = minTraceTime / 65536;
    }


    private long flags2bits(int flags) {
        long lfbits = 0L;

        if (0 != (flags & TracerLib.SUBMIT_TRACE)) {
            lfbits |= TF_SUBMIT_TRACE;
        } else if (0 != (flags & TracerLib.DROP_TRACE)) {
            lfbits |= TF_DROP_TRACE;
        } else if (0 != (flags & TracerLib.ERROR_MARK)) {
            lfbits |= TF_ERROR_MARK;
        }
        return lfbits;
    }


    @Override
    public void markTraceFlags(int traceId, int flags) {

        long lfbits = flags2bits(flags);

        if (lfbits != 0) {
            for (int i = stackPos - W0_OFF; i >= 0; i -= 3) {
                if ((int) ((stack[i] & TRACEID_MASK) >> TSTAMP_BITS) == traceId) {
                    stack[i] |= lfbits;
                    break;
                }
            }
        }
    }


    @Override
    public void markRecordFlags(int flags) {

        long lfbits = flags2bits(flags);

        if (lfbits != 0 && stackPos >= 3) {
            stack[stackPos-W0_OFF] |= lfbits;
        }
    }


    public boolean isInTrace(int traceId) {
        for (int i = stackPos - W0_OFF; i >= 0; i -= 3) {
            if ((int) ((stack[i] & TRACEID_MASK) >> TSTAMP_BITS) == traceId) {
                return true;
            }
        }
        return false;
    }



    private void extendStack() {
        stack = ZorkaUtil.clipArray(stack, stack.length + 384);
        stackLen = stack.length;
    }


    /** Returns current nano time. */
    protected long ticks() {
        return System.nanoTime() >> TICK_SHIFT;
    }

    /** Returns current wallclock time. */
    protected long clock() { return System.currentTimeMillis(); }


    protected void writeStringRef(String s) {
        writeUInt(CBOR.TAG_BASE, TAG_STRING_REF);
        writeUInt(CBOR.UINT_BASE, symbols.symbolId(s));
    }



    protected void dropTrace() {
        if (chunk != null) {
            if (chunk.getNext() != null) {
                bufManager.put(chunk.getNext());
                chunk.setNext(null);
            }
            buffer = chunk.getBuffer();
            bufPos = 0;
            bufOffs = 0;
            bufLen = buffer.length;
            nchunks = 1;
        } else {
            buffer = null;
            bufPos = 0;
            bufOffs = 0;
            bufLen = 0;
            nchunks = 0;
        }
    }


    public void flush() {

        if (traceEnabled) {
            log.trace("Flushing: bufLen=" + bufLen + ", bufPos=" + bufPos + ", bufOffs=" + bufOffs);
        }

        if (buffer != null) {
            flushChunk();
        }
        output.submit(chunk);
        chunk = null;

        bufOffs = 0;
    }


    protected void flushChunk() {
        if (buffer != null) {
            chunk.setPosition(bufPos);
            buffer = null;
            bufLen = 0;
            bufOffs += bufPos;
            bufPos = 0;
        }
    }


    protected void nextChunk() {
        if (buffer != null) {
            flushChunk();
        }
        STraceBufChunk ch = bufManager.get();
        ch.setStartOffset(0);
        ch.setExtOffset(bufOffs);
        ch.setNext(chunk);
        ch.setUuidL(uuidL);
        ch.setUuidH(uuidH);

        chunk = ch;
        buffer = ch.getBuffer();
        bufLen = buffer.length;
    }


    public void write(int b) {
        if (bufLen - bufPos < 1) nextChunk();
        buffer[bufPos++] = (byte)b;
    }

    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) {
        int bfree = bufLen - bufPos;

        if (bfree == 0) nextChunk();

        int sz = bfree < len ? bfree : len;

        System.arraycopy(b, off, buffer, bufPos, sz);
        bufPos += sz;

        if (sz < len) {
            write(b, off+sz, len-sz);
        }
    }


    public void writeUInt(int base, int i) {
        if (i < CBOR.UINT_CODE1) {
            if (bufLen - bufPos < 1) nextChunk();
            buffer[bufPos++] = (byte)(base+i);
        } else if (i < 0x100) {
            if (bufLen - bufPos < 2) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE1);
            buffer[bufPos+1] = (byte) (i & 0xff);
            bufPos += 2;
        } else if (i < 0x10000) {
            if (bufLen - bufPos < 3) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE2);
            buffer[bufPos+1] = (byte) ((i >> 8) & 0xff);
            buffer[bufPos+2] = (byte) (i & 0xff);
            bufPos += 3;
        } else {
            if (bufLen - bufPos < 5) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE4);
            buffer[bufPos+1] = (byte) ((i >> 24) & 0xff);
            buffer[bufPos+2] = (byte) ((i >> 16) & 0xff);
            buffer[bufPos+3] = (byte) ((i >> 8) & 0xff);
            buffer[bufPos+4] = (byte) (i & 0xff);
            bufPos += 5;
        }
    }

    public void writeULong(int base, long l) {
        if (l <= Integer.MAX_VALUE) {
            writeUInt(base, (int)l);
        } else {
            if (bufLen - bufPos < 9) nextChunk();
            buffer[bufPos] = (byte) (base+CBOR.UINT_CODE8);
            buffer[bufPos+1] = (byte) ((l >> 56) & 0xff);
            buffer[bufPos+2] = (byte) ((l >> 48) & 0xff);
            buffer[bufPos+3] = (byte) ((l >> 40) & 0xff);
            buffer[bufPos+4] = (byte) ((l >> 32) & 0xff);
            buffer[bufPos+5] = (byte) ((l >> 24) & 0xff);
            buffer[bufPos+6] = (byte) ((l >> 16) & 0xff);
            buffer[bufPos+7] = (byte) ((l >> 8) & 0xff);
            buffer[bufPos+8] = (byte)  (l & 0xff);
            bufPos += 9;
        }
    }

    public void writeString(String s)  {
        byte[] b = s.getBytes();
        writeUInt(0x60, b.length);
        write(b);
    }

    public void writeList(List lst) {
        // TODO obsłużyć również array of objects, array of integers itd.
        writeUInt(0x80, lst.size());
        for (Object itm : lst) {
            writeObject(itm);
        }
    }

    public void writeMap(Map<Object,Object> map) {
        writeUInt(0xa0, map.size());
        for (Map.Entry e : map.entrySet()) {
            writeObject(e.getKey());
            writeObject(e.getValue());
        }
    }

    public void writeInt(int i) {
        if (i >= 0) {
            writeUInt(0, i);
        } else {
            writeUInt(0x20, Math.abs(i)-1);
        }
    }

    public void writeLong(long l) {
        if (l >= 0) {
            writeULong(0, l);
        } else {
            writeULong(0x20, Math.abs(l)-1L);
        }
    }

    public void writeFloat(float f) {
        int i = Float.floatToIntBits(f);
        if (bufLen - bufPos < 5) nextChunk();
        buffer[bufPos] = (byte)CBOR.FLOAT_BASE4;
        buffer[bufPos+1] = (byte)((i >> 24) & 0xff);
        buffer[bufPos+2] = (byte)((i >> 16) & 0xff);
        buffer[bufPos+3] = (byte)((i >> 8) & 0xff);
        buffer[bufPos+4] = (byte)(i & 0xff);
        bufPos += 5;
    }

    public void writeDouble(double d) {
        if (bufLen - bufPos < 9) nextChunk();
        long l = Double.doubleToLongBits(d);
        buffer[bufPos] = (byte)CBOR.FLOAT_BASE8;
        buffer[bufPos + 1] = (byte)((l >> 56) & 0xff);
        buffer[bufPos + 2] = (byte)((l >> 48) & 0xff);
        buffer[bufPos + 3] = (byte)((l >> 40) & 0xff);
        buffer[bufPos + 4] = (byte)((l >> 32) & 0xff);
        buffer[bufPos + 5] = (byte)((l >> 24) & 0xff);
        buffer[bufPos + 6] = (byte)((l >> 16) & 0xff);
        buffer[bufPos + 7] = (byte)((l >> 8) & 0xff);
        buffer[bufPos + 8] = (byte)(l & 0xff);
        bufPos += 9;
    }

    public void writeObject(Object obj) {

        if (obj == null) {
            write(CBOR.NULL_CODE);
            return;
        }

        Class<?> c = obj.getClass();
        if (c == Byte.class || c == Short.class || c == Integer.class) {
            writeInt(((Number)obj).intValue());
        } else if (c == Long.class) {
            writeLong(((Number)obj).longValue());
        } else if (c == String.class) {
            writeString((String)obj);
        } else if (obj instanceof List) {
            writeList((List)obj);
        } else if (obj instanceof Map) {
            writeMap((Map)obj);
        } else if (Boolean.FALSE.equals(obj)) {
            write(CBOR.FALSE_CODE);
        } else if (Boolean.TRUE.equals(obj)) {
            write(CBOR.TRUE_CODE);
        } else if (obj == CBOR.BREAK) {
            write(CBOR.BREAK_CODE);
        } else if (c == Float.class) {
            writeFloat((Float)obj);
        } else if (c == Double.class) {
            writeDouble((Double)obj);
        } else if (obj == CBOR.UNKNOWN) {
            write(CBOR.UNKNOWN_CODE);
        } else {
            throw new RuntimeException("Unsupported data type: " + c);
        }
    }

    static {
        /* Determine byte order here. */
        byte[] b = new byte[2];
        UNSAFE.putShort(b, BYTE_ARRAY_OFFS, (short)0x0102);
        if (b[0] == 0x01) {
            // Big Endian
            TREC_HEADER = TREC_HEADER_BE;
            TREC_EPILOG = TAG_EPILOG_BE;
        } else {
            // Little endian
            TREC_HEADER = TREC_HEADER_LE;
            TREC_EPILOG = TAG_EPILOG_LE;
        }
    }

}
