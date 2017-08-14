/*
 * Copyright (c) 2012-2017 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.common.zico;

/**
 *
 */
public class TraceDataFormat {

    // Single-byte encodedd tags (potentially often used).

    public static final int TAG_STRING_DEF = 0x01;
    public static final int TAG_METHOD_DEF = 0x02;
    public static final int TAG_AGENT_ATTR = 0x03;

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

    public static final int TAG_PROLOG_RAW      = 0x0e;
    public static final int TAG_EPILOG_RAW      = 0x0f;

    // Two-byte encoded tags (less often used ones)
    public static final int TAG_TRACE_BEGIN    = 0x21; /** Trace begin marker */
    public static final int TAG_EXCEPTION      = 0x22;
    public static final int TAG_EXCEPTION_REF  = 0x23;
    public static final int TAG_KEYWORD        = 0x24;


    /** Trace ID (refers to trace UUID from text index) */
    public static final int TI_TRACE_ID = 0x01;  // Translatable, indexable

    /** Chunk number (first chunk has number 0) - suitable only for chunked traces. */
    public static final int TI_CHNUM    = 0x02;

    /** Offset (in bytes) of current chunk in trace data. */
    public static final int TI_CHOFFS   = 0x03;

    /** Chunk length (in bytes). */
    public static final int TI_CHLEN    = 0x04;

    /** Timestamp of the beginning of trace. */
    public static final int TI_TSTAMP   = 0x05;

    /** Trace duration. */
    public static final int TI_DURATION = 0x06;  // Indexable

    /** Trace type (refers to string from text index). */
    public static final int TI_TYPE     = 0x07;  // Translatable, indexable

    /** Number of trace records below current one. */
    public static final int TI_RECS     = 0x08;

    /** Number of method calles registered by tracer in subtree starting in current record. */
    public static final int TI_CALLS    = 0x09;

    /** Number of errors registered by tracer in current subtree (sdtarting with current method). */
    public static final int TI_ERRORS   = 0x0a;

    /** Trace flags */
    public static final int TI_FLAGS    = 0x0b;  // Translatable, indexable

    /** Beginning timestamp. */
    public static final int TI_TSTART   = 0x0c;

    /** Ending timestamp. */
    public static final int TI_TSTOP    = 0x0d;

    /** Method ID. */
    public static final int TI_METHOD   = 0x0e;  // Translatable


    public static final int TI_SKIP     = 0x0f;

    /** Marks trace as ending with error (th. thrown exception). */
    public static final int TF_ERROR    = 0x01; // Error flag

    //
    public static final int TRACE_DROP_TOKEN   = 0xe0; /* TRACE DROP is encoded as simple value. */

    /** This is pre-computed 4-byte trace record header. */

    public static final int TREC_HEADER_BE = 0xd80a9f48;
    public static final int TREC_HEADER_LE = 0x489f0bd8;

    public static final byte STRING_TYPE  = 0x00; // Generic string, raw encoding (no prefix);

    public static final byte TYPE_MIN     = 0x04;
    public static final byte KEYWORD_TYPE = 0x04; // LISP keyword:     0x04|keyword_name|0x04
    public static final byte CLASS_TYPE   = 0x05; // Class name        0x05|class_name|0x05
    public static final byte METHOD_TYPE  = 0x06; // Method name       0x06|method_name|0x06
    public static final byte UUID_TYPE    = 0x07; // UUID              0x07|uuid_encoded|0x07
    public static final byte SIGN_TYPE    = 0x08; // Method signature  0x08|method_signature|0x08
    public static final byte TYPE_MAX     = 0x08;

}
