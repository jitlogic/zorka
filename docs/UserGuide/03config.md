
# Configuring Zorka

Zorka agent can be configured using `zorka.properties` file and by beanshell scripts placed in `$ZORKA_HOME/conf`
directory. Common agent settings and supplied scripts (with their specific settings) have been described in this
section. For more information about implementing your own BSH scripts, see `API Reference` section.

## Basic configuration settings

Core agent configuration settings from `zorka.properties` are described below (along with some sample values).

### General agent settings

* `zorka.mbs.autoregister = yes` - this setting controls whether standard platform mbean server should be automatically
registered at first use (or startup time): this is useful when application server substitutes mbean server at startup
time and it has be acquired in other manner than at startup (JBoss 4/5/6 do this, so both 'java' and 'jboss' mbean
servers are acquired by zorka5.sar module instead of `ManagementFactory.getPlatformMBeanServer()`;

* `zorka.hostname = tomcat.myserver` - 'hostname' this agent will advertise itself; this is useful for for automatic
informing operating system about server hostname (eg. standard Zabbix Agent template has such item);

Zabbix related directives:

* `zabbix.enabled = yes` - this setting controls whether Zorka should work as zabbix agent (serving zabbix agent
protocol on some address:port); zabbix protocol is the only one supported at the moment;

* `zabbix.listen.addr = 0.0.0.0` - address zabbix agent will listen on (`127.0.0.1` is the default value, so it has to
be set explicitly);

* `zabbix.listen.port = 10055` - port zabbix agent will listen on;

* `zabbix.server.addr = 127.0.0.1` - zabbix server address; agent will accept connections only from this server;

Logging directives:

* `zorka.log.level = TRACE` - overall log level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` are allowed);

* `zorka.log.size = 4m` - maximum size of log files (in megabytes if `m` is added as a suffix);

* `zorka.log.num = 4` - maximum number of archived log files;

* `zorka.log.exceptions = yes` - controls whether full stack traces of encountered exceptions will be logged;

Logging directives related to syslog:

* `zorka.syslog = no` - enables sending zorka logs to remote syslog server if set to `yes`;

* `zorka.syslog.server` - IP address and (optional) port number of syslog server in `addr:port` form;

* `zorka.syslog.facility` - syslog facility code each message will be tagged with (eg. `F_LOCAL0`);

Directives useful for tuning (you propably don't need to change default values):

* `zorka.req.threads = 4` - number of threads for processing requests;

* `zorka.req.queue = 64` -

* `zorka.req.timeout = 10000` - request timeout (in milliseconds) - requests taking more than this will be canceled
and zorka will close connection abruply;

* `syslog = yes` - controls whether enable (or disable) syslog support; syslog protocol is enabled by default;

* `spy = yes` - enables or disables Zorka Spy (spy is enabled by default);

* `spy.debug = 1` - sets verbosity for instrumentation engine;

* `tracer = yes` - enables tracer;

* `perfmon = yes` - enables

### Logging directives

Zorka allows for more precise logging configuration. Selected aspects of various subsystems can be turned on or off.
Supply comma-separated flags in configuration file to configure logging of some subsystem in a precise way.

#### Agent core log flags

    zorka.log.agent = INFO

The following flags are available:

* `NONE` - turn off all messages from agent core;

* `CONFIG` - agent configuration information;

* `QUERIES` - logs every query handled (and its result);

* `TRACES` - logs detailed (trace) messages from agent core;

* `WARNINGS` - logs warning messages from agent core;

* `ERRORS` - logs error messages from agent core;

Additional grouping flags have been added for convenience:

* `INFO` = `CONFIG`, `ERRORS`      (default setting)

* `DEBUG` = `INFO`, `QUERIES`

* `TRACE` = `DEBUG`, `TRACES`

#### Spy engine log flags

    zorka.log.spy = INFO

The following flags are available:

* `NONE` - turn off all messages from spy engine;

* `ARGPROC` - argument processing information (for instrumented code);

* `CONFIG` - spy configuration information;

* `SUBMIT` - argument fetch and submission information (for instrumented code);

* `ERRORS` - tracer errors

* `CLASS_DBG` - class-level debug information;

* `METHOD_DBG` - method-level debug information;

* `CLASS_TRC` - class-level trace information;

* `METHOD_TRC` - method-level trace information;

Additional grouping flags have been added for convenience:

* `INFO` = `CONFIG`, `ERRORS`      (default setting)

* `DEBUG` = `INFO`, `CLASS_DBG`, `METHOD_DBG`

* `TRACE` = `DEBUG`, `SUBMIT`, `ARGPROC`, `CLASS_TRC`, `METHOD_TRC`

#### Tracer log flags

    zorka.log.tracer = INFO

This configures tracer logging flags. The following flags are available:

* `NONE` - turn off all log messages related to tracer;

* `CONFIG` - tracer configuration information;

* `INSTRUMENT_CLASS` - class-level messages from instrumentation engine;

* `INSTRUMENT_METHOD` - method-level messages from instrumentation engine;

* `SYMBOL_REGISTRY` - log changes in symbol registry;

* `SYMBOL_ENRICHMENT` - log which symbols are to be sent to output trace file;

* `TRACE_ERRORS` - log internal errors of tracer engine;

* `TRACE_CALLS` - log each and every call of tracer engine (from instrumented methods);

Additional grouping flags have been added for convenience:

* `INFO` = `CONFIG`, `TRACE_ERRORS`

* `DEBUG` = `INFO`, `INSTRUMENT_CLASS`, `INSTRUMENT_METHOD`

* `TRACE` = `DEBUG`, `SYMBOL_REGISTRY`, `SYMBOL_ENRICHMENT`

* `TRACE_FULL` = `TRACE`, `TRACE_CALLS`

#### Performance monitor flags

    zorka.log.perfmon = INFO

This configures performance monitor flags. The following flags are available:

* `NONE` - turn off all logging related to performance monitor;

* `CONFIG` - performance monitor configuration information;

* `RUNS` - logs a message every time some performance monitor starts new data collection cycle;

* `RUN_DEBUG` - logs more debug information on performance monitor run cycles;

* `RUN_TRACE` - logs all information on performance monitor run cycles (including collected samples);

* `ERRORS` - logs errors that occur while running performance monitor;

Additional grouping flags have been added for convenience:

* `INFO` = `CONFIG`, `ERRORS`

* `DEBUG` = `INFO`, `RUNS`, `RUN_DEBUG`

* `TRACE` = `DEBUG`, `RUN_TRACE`



## Extension scripts & Zabbix templates

With no configuration, agent has fairly limited capabilities as most of its functionality is configured via extension
scripts. While writing extension scripts requires fair amount of knowledge about agent internals, there is a bunch
of ready to use scripts distributed with agent. Scripts may (but don't have to) use configuration parameters that can
be set in `zorka.properties` file. In order to enable an extension script, copy it to `${zorka.home.dir}/conf` directory.

Some configuration parameters are used across many scripts (and some are even used by agent core):

* `tracer` - enables or disables method call tracing configuration, if given script contains one; it is also used by
agent core to enable or disable tracer subsystem itself;

* `tracer.verbose = no` - increases tracer verbosity; eg. all started traces can be logged in zorka log;

* `tracer.log.path = ${zorka.log.dir}/trace.trc` - path to tracer files;

* `tracer.log.fnum = 8` - number of archive files (trace files are rotated to keep local filesystem from overflow);

* `tracer.log.size = 128M` - maximum size of single trace file;

* `tracer.min.method.time` - minimum time method must execute to be included in trace;

* `tracer.min.trace.time` - minimum time trace must execute to be logged;

* `tracer.max.trace.records` - maximum number of method execution records to be included in a single trace;

* `perfmon` - enables or disables recording of performance metrics;

* `perfmon.interval` - interval between performance monitor cycles;

### jvm.bsh - basic JVM parameters and monitoring

This script defines `jvm` namespace with the following helper functions:

* `jvm.memutil(name)` - memory pool utilization (by name);

* `jvm.heaputil(name)` - memory heap utilization (by name);

If `perfmon` is enabled, basic JVM performance metrics will be collected. Tracer output has to be configured in order
to save those metrics to a file. With zabbix, this script can be used with `Template_Zorka_JVM.xml` template.

### zabbix.bsh - Zabbix support

This script defines basic items emulating zabbix agent, so standard `Template Zabbix Agent` will work properly.

### tomcat.bsh - Apache Tomcat support

This script defines `tomcat` namespace containing set of helper functions that can be used in conjunction with the
following zabbix templates:

* `Template_Zorka_Tomcat_RequestProcessors` - request processing metrics;

* `Template_Zorka_Tomcat_Servlets` - servlet execution metrics;

* `Template_Zorka_Tomcat_JSP` - information about JSP (statuses, reloads);

* `Template_Zorka_Tomcat_ThreadPools` - thread pools utilization metrics;

This script will also configure tracer for Tomcat, if `tracer` parameter is set to `yes` in `zorka.properties`.
Additional `tracer.*` parameters are also in effect.

### jboss.bsh - JBoss 4/5/6 support

This script defines `jboss` namespace containing set of helper functions that are used in conjunction with the following
zabbix templates:

* `Template_Zorka_JBoss` - basic information about JBoss;

* `Template_Zorka_JBoss_EJB3` - EJB3 metrics;

* `Template_Zorka_JBoss_JCA` - JCA metrics;

* `Template_Zorka_JBoss_MQ` - MQ metrics;

* `Template_Zorka_JBoss_RequestProcessors` - request processing metrics;

* `Template_Zorka_JBoss_Servlets` - servlet execution metrics;

This script will also configure tracer for Tomcat, if `tracer` parameter is set to `yes` in `zorka.properties`.
Additional `tracer.*` parameters are also in effect.

### jboss7.bsh - JBoss 7.x support

This script defines `jboss7` namespace containing set of helper functions that are used in conjunction with the following
zabbix templates:

* `Template_Zorka_JBoss7_Ejb3Stateless` - EJB3 metrics;

* `Template_Zorka_JBoss7_JpaPersistence` - Hibernate persistence metrics;

* `Template_Zorka_JBoss7_RequestProcessors` - request processing metrics;

* `Template_Zorka_JBoss7_Servlets` - setvlet execution metrics;

* `Template_Zorka_JBoss7_Sessions` - http session statistics;

This script will also configure tracer for Tomcat, if `tracer` parameter is set to `yes` in `zorka.properties`.
Additional `tracer.*` parameters are also in effect.

### Other templates

The following zabbix templates don't require extension scripts:

* `Template_Zorka_Diagnostics` - useful template monitoring zorka agent itself; it collects statistics about various
agent subsystems and defines some triggers alerting if something in the agent goes wrong;

* `Template_Zorka_ActiveMQ`, `Template_Zorka_ApacheSynapse`, `Template_Zorka_Mule` - templates for Apache ActiveMQ,
Apache Synapse and Mule ESB;

