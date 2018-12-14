/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.netkit.http;

public enum HttpDecoderState {

    ALL_READ(false, true),
    CONNECTION_OPEN(false, false),
    READ_CHUNKED_CONTENT(false, false),
    READ_CHUNK_DELIMITER(false, false),
    READ_CHUNK_FOOTER(false, false),
    READ_CHUNK_SIZE(false, false),
    READ_FIXED_LENGTH_CONTENT(false, false),
    READ_HEADER(true, false),
    READ_REQ_LINE(true, false),
    READ_RESP_LINE(true, false),
    READ_VARIABLE_LENGTH_CONTENT(false, false),
    ERROR(false, true);

    public final boolean isLine;
    public final boolean isTerminal;

    HttpDecoderState(boolean isLine, boolean isTerminal) {
        this.isLine = isLine;
        this.isTerminal = isTerminal;
    }

}
