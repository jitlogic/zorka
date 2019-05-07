package com.jitlogic.zorka.cbor;

/**
 * Set of standard attribute names for traces.
 */
public class TraceAttributes {

    /** Old trace type, adjusted to OpenTracing convention */
    public static final String COMPONENT = "component";


    public static final String CALL_METHOD = "call.method";

    public static final String LOCAL_HOSTNAME = "local.hostname";
    public static final String LOCAL_SERVICE = "local.service";
    public static final String LOCAL_APP = "local.app";
    public static final String LOCAL_ENV = "local.env";

    /** Virtual attribute, generated on the fly when retrieving traces. */
    public static final String TRACE_DESC = "trace.description";

}
