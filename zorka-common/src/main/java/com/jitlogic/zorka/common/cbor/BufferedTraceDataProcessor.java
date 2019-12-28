package com.jitlogic.zorka.common.cbor;

public interface BufferedTraceDataProcessor extends TraceDataProcessor {

    int size();

    byte[] chunk(int offs, int len);
}
