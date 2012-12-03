
# Configuring Zorka

Zorka exposes its API as modules with functions in a Beanshell interpreter. Zabbix server can call these functions
directly (with slightly different syntax), extension scripts can use them as well (and define their own functions
that will be visible for Zabbix or other monitoring system.

Beanshell scripts are primary way to configure and extend agent. Zorka executes all beanshell scripts from
`$ZORKA_HOME/conf` directory at startup (in alphabetical order). All elements declared in those scripts will
be visible via queries from monitoring servers. User can define his own namespaces, functions. Declaring which
elements should be instrumented (and how) is also possible via `.bsh` scripts.

## zorka.properties configuration file

Interesting configuration directives in zorka.properties are described below (along with some sample values).

Zorka agent general directives:

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

* `zorka.log.level = TRACE` - log level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` are allowed);

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


## Basic API

Zorka exposes standard BeanShell environment (with access to all classes visible from system class loader plus all
Zorka code). There are several library objects visible:

* `zorka` - zorka-specific library functions (logging, JMX access etc., always available);

* `spy` - functions for configuring instrumentation engine (available if spy is enabled);

* `zabbix` - zabbix-specific functions (available if zabbix interface is enabled);

* `nagios` - nagios-specific functions (available if nagios interface is enabled);

* `syslog` - functions for sending messages to log host using syslog protocol;

All above things are visible Beanshell scripts from `$ZORKA_HOME/conf` directory. Interfaces to monitoring systems
(zabbix, nagios etc.) can call Beanshell functions as well (both built-in and user-defined) but due to their syntax
are typically limited to simple calls. For example:

    zorka.jmx["java","java.lang:type=OperatingSystem","Arch"]

is equivalent to:

    zorka.jmx("java","java.lang:type=OperatingSystem","Arch");


## MBean servers

Zorka can track many mbean servers at once. This is useful for example in JBoss 4/5/6 which have two mbean servers:
platform specific (JVM) and application server specific (JBoss). Each mbean server is available at some name.
Known names:

* `java` - standard plaform mbean server;

* `jboss` - JBoss JMX kernel mbean server;

MBean server name is passed in virtually all functions looking for object names or registering/manipulating
objects/attributes in JMX.
