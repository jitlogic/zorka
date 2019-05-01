package com.jitlogic.zorka.cbor;

public class TraceDataTags {

    // Wire format

    public static final int TAG_STRING_DEF = 0x01;
    public static final int TAG_METHOD_DEF = 0x02;

    /** String reference is a tagged number. */
    public static final int TAG_STRING_REF     = 0x06;

    /** Trace record start */
    public static final int TAG_TRACE_START    = 0x08;

    /** Trace attribute */
    public static final int TAG_TRACE_ATTR     = 0x09;

    public static final int TAG_PROLOG_BE = 0x0a;
    public static final int TAG_PROLOG_LE = 0x0b;
    public static final int TAG_EPILOG_BE = 0x0c;
    public static final int TAG_EPILOG_LE = 0x0d;

    public static final int TAG_TRACE_INFO  = 0x0e;
    public static final int TAG_TRACE_FLAGS = 0x0f;

    // Two-byte encoded tags (less often used ones)
    public static final int TAG_TRACE_BEGIN    = 0x21; /** Trace begin marker */
    public static final int TAG_EXCEPTION      = 0x22;
    public static final int TAG_EXCEPTION_REF  = 0x23;


    public static final int TAG_TRACE_UP_ATTR = 0x26;

}
