package com.jitlogic.zorka.cbor;

/**
 * Set of standard attribute names for traces.
 */
public class TraceAttributes {

    public static final String CALL_METHOD = "call.method";

    /** Old trace type, adjusted to OpenTracing convention */
    public static final String COMPONENT = "component";

    public static final String C_BREAKER = "breaker";
    public static final String C_CALL    = "call";
    public static final String C_CAMEL   = "camel";
    public static final String C_DB      = "db";
    public static final String C_FLEX    = "flex";
    public static final String C_JOB     = "job";
    public static final String C_HTTP    = "http";
    public static final String C_REMOTE  = "remote";
    public static final String C_REST    = "rest";
    public static final String C_SOAP    = "soap";
    public static final String C_SPRING  = "spring";

    public static final String DB_INSTANCE = "db.instance";
    public static final String DB_STATEMENT = "db.statement";
    public static final String DB_TYPE = "db.type";
    public static final String DB_USER = "db.user";
    public static final String DB_URL = "db.url";

    public static final String HTTP_METHOD = "http.method";
    public static final String HTTP_STATUS = "http.status_code";
    public static final String HTTP_URL = "http.url";

    public static final String ERROR = "error";

    public static final String LOCAL_ADDRESS = "local.address";
    public static final String LOCAL_HOSTNAME = "local.hostname";
    public static final String LOCAL_IPV4 = "local.ipv4";
    public static final String LOCAL_IPV6 = "local.ipv6";
    public static final String LOCAL_PID = "local.pid";
    public static final String LOCAL_SERVICE = "local.service";

    public static final String MSG_DESTINATION = "message_bus.destination";

    public static final String PEER_HOSTNAME = "peer.hostname";
    public static final String PEER_SERVICE = "peer.service";
    public static final String PEER_ADDRESS = "peer.address";
    public static final String PEER_IPV4 = "peer.ipv4";
    public static final String PEER_IPV6 = "peer.ipv6";

    public static final String SAMPLING_PRIORITY = "sampling.priority";

    public static final String SPAN_KIND = "span.kind";

    // Standard values of span.kind attribute
    public static final String SK_JOB = "JOB";
    public static final String SK_BOOT = "BOOT";
    public static final String SK_CLIENT = "CLIENT";
    public static final String SK_SERVER = "SERVER";
    public static final String SK_PRODUCER = "PRODUCER";
    public static final String SK_CONSUMER = "CONSUMER";
    public static final String SK_COMPONENT = "COMPONENT";


    /** Virtual attribute, generated on the fly when retrieving traces. */
    public static final String TRACE_DESC = "trace.description";
}
