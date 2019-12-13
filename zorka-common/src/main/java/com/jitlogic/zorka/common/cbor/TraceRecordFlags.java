package com.jitlogic.zorka.common.cbor;

public class TraceRecordFlags {

    /** Force trace submission */
    public static final int TF_SUBMIT_TRACE  = 0x01;

    /** Force current method submission */
    public static final int TF_SUBMIT_METHOD = 0x02;

    /** Force trace drop */
    public static final int TF_DROP_TRACE    = 0x04;

    /** Marks trace as ending with error (th. thrown exception). */
    public static final int TF_ERROR_MARK = 0x08;

    /** If set, chunk is only part of whole trace. */
    public static final int TF_CHUNK_ENABLED = 0x10;

    /** Marks first chunk of trace. */
    public static final int TF_CHUNK_FIRST = 0x20;

    /** Marks final chunk of trace. */
    public static final int TF_CHUNK_LAST = 0x40;

    // Do not instantiate this class.
    private TraceRecordFlags() { }
}
