package com.jitlogic.zorka.common.cbor;

/**
 * Keys for TraceInfo records as stored in TDB storage.
 */
public class TraceInfoConstants {

    /** Chunk number (first chunk has number 0) - suitable only for chunked traces. */
    public static final int TI_CHNUM    = 0x02;

    /** Offset (in bytes) of current chunk in trace data. */
    public static final int TI_CHOFFS   = 0x03;

    /** Chunk length (in bytes). */
    public static final int TI_CHLEN    = 0x04;

    /** Timestamp of the beginning of trace. */
    public static final int TI_TSTAMP   = 0x05;

    /** Trace duration. */
    public static final int TI_DURATION = 0x06;

    /** Number of trace records below current one. */
    public static final int TI_RECS     = 0x08;

    /** Number of method calles registered by tracer in subtree starting in current record. */
    public static final int TI_CALLS    = 0x09;

    /** Number of errors registered by tracer in current subtree (sdtarting with current method). */
    public static final int TI_ERRORS   = 0x0a;

    /** Trace flags */
    public static final int TI_FLAGS    = 0x0b;

    /** Beginning timestamp. */
    public static final int TI_TSTART   = 0x0c;

    /** Ending timestamp. */
    public static final int TI_TSTOP    = 0x0d;

    /** Method ID. */
    public static final int TI_METHOD   = 0x0e;

    /** Clear trace flags. */
    public static final int TI_FLAGS_C  = 0x10;

    /** Parent spanId */
    public static final int TI_PARENT   = 0x11;

    /** spanId */
    public static final int TI_SPAN     = 0x12;
}
