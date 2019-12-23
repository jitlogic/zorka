package com.jitlogic.zorka.common.cbor;

public class TraceDataTags {

    // Agent data tags
    public static final int TAG_STRING_DEF = 0x01;
    public static final int TAG_METHOD_DEF = 0x02;

    // Trace data tags
    public static final int TAG_TRACE_START   = 0x03;
    public static final int TAG_TRACE_END     = 0x04;
    public static final int TAG_TRACE_BEGIN   = 0x05;
    public static final int TAG_TRACE_ATTR    = 0x06;
    public static final int TAG_EXCEPTION     = 0x07;
    public static final int TAG_EXCEPTION_REF = 0x08;
}
