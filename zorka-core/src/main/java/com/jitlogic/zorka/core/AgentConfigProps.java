package com.jitlogic.zorka.core;

import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Set;

public class AgentConfigProps {

    public static final String SCRIPTS_PROP = "scripts";

    public static final String SCRIPTS_AUTO_PROP = "scripts.auto";
    public static final boolean SCRIPTS_AUTO_DVAL = false;

    public static final String SCRIPTS_DIR_PROP = "zorka.scripts.dir";
    public static final String SCRIPTS_DIR_DVAL = "scripts";

    /** Enables or disables spy instrumentation. */
    public static final String SPY_PROP = "spy";
    public static final boolean SPY_DEFV = true;

    /** Enables or disables syslog subsystem. */
    public static final String SYSLOG_PROP = "syslog";
    public static final boolean SYSLOG_DEFV = true;

    public static final String TRACER_DISTRIBUTED_PROP = "tracer.distributed";
    public static final boolean TRACER_DISTRIBUTED_DEFV = false;

    /** Maximum number of chunks that can be buffered by trace handler (and then sent together). */
    public static final String TRACER_CHUNK_MAX_PROP = "tracer.chunk.max";
    public static final int TRACER_CHUNK_MAX_DEFV = 16;

    public static final String TRACER_CHUNK_SIZE_PROP = "tracer.chunk.size";
    public static final int TRACER_CHUNK_SIZE_DEFV = 65536;

    /** Enables or disables tracer. */
    public static final String TRACER_PROP = "tracer";
    public static final boolean TRACER_DEFV = false;

    /** When set to true, chunks can be sent even if trace is incomplete.
        When false, chunks will always be grouped together. */
    public static final String TRACER_STREAMING_CHUNKED_PROP = "tracer.streaming.chunked";
    public static final boolean TRACER_STREAMING_CHUNKED_DEFV = false;

    /** Enables or disables tracer tuner. */
    public static final String TRACER_TUNER_PROP = "tracer.tuner";
    public static final boolean TRACER_TUNER_DEFV = true;

    /** Enables or disables automatic reinstrumentation of tracer classes. */
    public static final String TRACER_TUNER_AUTO_PROP = "tracer.tuner.auto";
    public static final boolean TRACER_TUNER_AUTO_DEFV = true;

    /** Tracer tuner directory. */
    public static final String TRACER_TUNER_DIR_PROP = "tracer.tuner.dir";
    public static final String TRACER_TUNER_DIR_DEFV = "tuner";

    /** Interval between tracer tuner cycles. */
    public static final String TRACER_TUNER_INTERVAL_PROP = "tracer.tuner.interval";
    public static final int TRACER_TUNER_INTERVAL_DEFV = 30000;

    /** Maximum number of methods to be excluded in one tuning cycle. */
    public static final String TRACER_TUNER_MAX_ITEMS_PROP = "tracer.tuner.max.items";
    public static final int TRACER_TUNER_MAX_ITEMS_DEFV = 512;

    /** Maximum percentage of method calls to be excluded in one tuning cycle. */
    public static final String TRACER_TUNER_MAX_RATIO_PROP = "tracer.tuner.max.ratio";
    public static final int TRACER_TUNER_MAX_RATIO_DEFV = 80;

    /** Minimum number of calls that will qualify method for exclusion. */
    public static final String TRACER_TUNER_MIN_RANK_PROP = "tracer.tuner.min.rank";
    public static final long TRACER_TUNER_MIN_RANK_DEFV = 10000L;

    /** Minimum number of calls registered by tracer (globally) that will trigger reinstrumentation. */
    public static final String TRACER_TUNER_MIN_CALLS_PROP = "tracer.tuner.min.calls";
    public static final long TRACER_TUNER_MIN_CALLS_DEFV = 1000000L;

    /** Tracer tuner queue length. */
    public static final String TRACER_TUNER_QLEN_PROP = "tracer.tuner.qlen";
    public static final int TRACER_TUNER_QLEN_DEFV = 1024;

    /** Tracer tuner ranking size: how many methods will make it to ranking. */
    public static final String TRACER_TUNER_RANKS_PROP = "tracer.tuner.ranks";
    public static final int TRACER_TUNER_RANKS_DEFV = 1023;

    /** Tuner exclusion log file (all new exclusions will be added here automatically). */
    public static final String TRACER_TUNER_XLOG_PROP = "tracer.tuner.xlog";
    public static final String TRACER_TUNER_XLOG_DEFV = "_log.ztx";

    public static final String TRACER_TUNER_ZTX_SCAN_PROP = "tracer.tuner.ztx.scan";
    public static final boolean TRACER_TUNER_ZTX_SCAN_DEFV = true;


    /** Selects tracer type. */
    public static final String TRACER_TYPE_PROP = "tracer.type";
    public static final String TRACER_TYPE_LOCAL = "local";
    public static final String TRACER_TYPE_STREAMING = "streaming";

    /** Enables or disables zabbix agent (standard) protocol. */
    public static final String ZABBIX_PROP = "zabbix";
    public static final boolean ZABBIX_DEFV = true;

    /** Enables or disables zabbix agent (active) protocol. */
    public static final String ZABBIX_ACTIVE_PROP = "zabbix.active";
    public static final boolean ZABBIX_ACTIVE_DEFV = false;

    /** Prefix for all zorka agent prefixes. */
    public static final String ZORKA_AGENT_PROP = "zorka.agent";

    /** Diagnostic data mbean object name. */
    public static final String ZORKA_DIAGNOSTICS_MBEAN = "zorka.diagnostics.mbean";

    public static final String ZORKA_HOME_DIR_PROP = "zorka.home.dir";

    /** Agent name. Used by zabbix, tracer and all. */
    public static final String ZORKA_HOSTNAME_PROP = "zorka.hostname";
    public static final String ZORKA_HOSTNAME_DVAL = "zorka";

    /** Zorka logs directory. */
    public static final String ZORKA_LOG_DIR_PROP = "zorka.log.dir";
    public static final String ZORKA_LOG_DIR_DVAL = "log";

    /** Enables or disables agent log file output. */
    public static final String ZORKA_LOG_FILE_PROP = "zorka.log.file";
    public static final boolean ZORKA_LOG_FILE_DVAL = true;

    public static final String ZORKA_LOG_FILE_NAME_PROP = "zorka.log.file.path";
    public static final String ZORKA_LOG_FILE_NAME_DVAL = "zorka.log";

    public static final String ZORKA_LOG_FILE_NUM_PROP = "zorka.log.file.num";
    public static final int ZORKA_LOG_FILE_NUM_DVAL = 8;

    public static final String ZORKA_LOG_FILE_SIZE_PROP = "zorka.log.file.size";
    public static final long ZORKA_LOG_FILE_SIZE_DVAL = 16L * 1024 * 1024;

    public static final String ZORKA_LOG_LEVEL_PROP = "zorka.log.level";
    public static final String ZORKA_LOG_LEVEL_DVAL = "INFO";

    /** Enables or disables agent logs syslog output. */
    public static final String ZORKA_LOG_SYSLOG_PROP = "zorka.log.syslog";
    public static final boolean ZORKA_LOG_SYSLOG_DVAL = false;

    public static final String ZORKA_LOG_CONSOLE_PROP = "zorka.log.console";
    public static final boolean ZORKA_LOG_CONSOLE_DVAL = false;

    public static final String ZORKA_LOG_SYSLOG_ADDR_PROP = "zorka.log.syslog.addr";
    public static final String ZORKA_LOG_SYSLOG_ADDR_DVAL = "127.0.0.1";

    public static final String ZORKA_LOG_SYSLOG_FACILITY_PROP = "zorka.log.syslog.facility";
    public static final String ZORKA_LOG_SYSLOG_FACILITY_DVAL = "F_LOCAL0";

    public static final String ZORKA_LOG_FSYNC_PROP = "zorka.log.fsync";
    public static final boolean ZORKA_LOG_FSYNC_DVAL = false;

    public static final String ZORKA_PREFIX_PROP = "zorka.prefix";

    /** Request queue length. */
    public static final String ZORKA_REQ_QUEUE_PROP = "zorka.req.queue";
    public static final int ZORKA_REQ_QUEUE_DEFV = 256;

    /** Number of request handling threads. */
    public static final String ZORKA_REQ_THREADS_PROP = "zorka.req.threads";
    public static final int ZORKA_REQ_THREADS_DEFV = 16;

    /** Request timeout. */
    public static final String ZORKA_REQ_TIMEOUT_PROP = "zorka.req.timeout";
    public static final long ZORKA_REQ_TIMEOUT_DEFV = 5000L;

    public static final AgentConfigProps PROPS = new AgentConfigProps();

    private AgentConfigProps() {
    }
}
