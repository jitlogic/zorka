# Introduction


Zorka is a powerful general purpose monitoring agent for Java application servers. Zorka does not provide its own
collector and is intended to integrate seamlessly with existing monitoring systems (with Zabbix support being
implemented at first):

* integration with commonly used monitoring systems: Zabbix, (Nagios planned);

* programmability - zorka can be extended using BeanShell scripts;

* bytecode instrumentation - zorka can instrument your code in several ways and present collected values via JMX Beans;

* mapped mbeans - user can map calcluated values from some mbeans into other mbeans (on attribute basis);
standard JMX clients can fetch these values;

* rank lists - customizable thread ranks, mbean ranks etc.

Note that this is development snapshot of Zorka agent. While it works fairly well on author's production workloads,
yet documentation is incomplete, its configuration directives and BSH APIs are still changing and getting it to work
might be a bit of challenge. In other works - it is still in flux - it's usable but more advanced feature might require
a bit of patience in order to get them working.



# Installation and configuration

Unpack zorka archive into application server home directory (ex. `/opt/tomcat-7.0.29`).
Zorka files will reside in `<server-home>/zorka` directory.  Unpacked directory contains
the following things:


* `agent.jar` - agent jar file (with all dependencies included);

* `conf/*.bsh` - extension scripts;

* `log/*.log` - log files;

* `home.*` - sample home directories -  (containing predefined `zorka.properties+conf/*.bsh` for various application
servers);

* `zorka.properties` - main configuration file;


## Application server configuration

Zorka works as java agent, thus the following thing has to be added to java command line:

    -javaagent:/path/to/zorka/agent.jar=/path/to/zorka

Also, some specific things may have to be added for specific application servers. In all examples below `<server-home>`
means application server home (eg. `/opt/jboss-4.2`) and `<zorka-home>` means directory with unpacked zorka files
(eg. `/opt/jboss-4.2/zorka`).

Some application servers have been (preliminarily) tested and are described below but for other servers should work as
well as long as application server does not perform too many wizardry on platform mbean server or class loaders.

### Tomcat 6.x, 7.x

Only Sun JDK 6/5 is supported with Tomcat at the moment:

* copy `samples/tomcat67/*` files to `<server-home>/zorka` directory (overwrite existing files if necessary);

* edit `<server-home>/bin/catalina.conf` and add the following line at the end:


    CATALINA_OPTS="$CATALINA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"

* adjust zorka.properties if necessary (log files, listen port number etc.);


### Jboss 4.x, 5.x, 6.x

Only Sun JDK 5 and 6 are supported:

* copy `samples/jboss456/*` files to `<server-home>/zorka` directory;

* edit `<server-home>/bin/run.conf` and add the following line at the end:


    JAVA_OPTS="$JAVA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"

* adjust zorka.properties if necessary (log files, listen port number etc.);

* import `Template_Zorka_JBoss_*.xml` templates into Zabbix;



### JBoss 7.x

Following configuration steps have been tested in standalone mode but should work the same in both modes
(edit `domain.conf` instead of `standalone.conf`).

* copy `samples/jboss7/*` files to `<server-home>/zorka` directory;

* make sure that `zorka.mbs.autoregister = yes` is set in `zorka.properties` file;

* edit `<server-home>/bin/standalone.conf` and add the following line:


    JAVA_OPTS="$JAVA_OPTS -javaagent:<zorka-home>/agent.jar=<zorka-home>"


* add zorka's bootstrap package to JBoss system modules - find and change the setting in `standalone.conf` file:


    if [ "x$JBOSS_MODULES_SYSTEM_PKGS" = "x" ]; then
       JBOSS_MODULES_SYSTEM_PKGS="org.jboss.byteman,com.jitlogic.zorka.spy"
    fi

* import `Template_Zorka_JBoss7_*.xml` templates into Zabbix;


### Mule ESB

Zorka has been tested with Mule ESB 3.3.0 working on Sun JDK 6 on Linux. Mule ESB uses Tanuki Software wrapper
configured via `$MULE_CONF/conf/wrapper.conf` file. In order to configure zorka:

* unpack zorka to `$MULE_HOME`;

* edit `wrapper.conf` file and add java agent to java arguments:


    wrapper.java.additional.8=-javaagent:$MULE_HOME/zorka/agent.jar=$MULE_HOME/zorka
    wrapper.java.additional.8.stripquotes=TRUE

* make sure that `zorka.mbs.autoregister = yes` is set in `zorka.properties` file;

* import `Template_Zorka_Mule*.xml` templates into Zabbix;


### Apache ActiveMQ

Zorka has been tested with ActiveMQ 5.6.0 working on SunJDK6 on Linux. Wrapper has its configuration files in
`$MQ_HOME/bin/linux-*` directories - so you have to modify one of `wrapper.conf` files  - depending on what
architecture are you running for:

* edit wrapper.conf file and add java agent to java arguments (adjust option number):


    wrapper.java.additional.12=-javaagent:%ACTIVEMQ_HOME%/zorka/agent.jar=%ACTIVEMQ_HOME%/zorka

* make sure that `zorka.mbs.autoregister = yes` is set in `zorka.properties` file;

* import `Template_Zorka_ActiveMQ*.xml` templates into Zabbix;


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

Directives useful for tuning (you propably don't need to change default values):

* `zorka.req.threads = 4` - number of threads for processing requests;

* `zorka.req.queue = 64` -

* `zorka.req.timeout = 10000` - request timeout (in milliseconds) - requests taking more than this will be canceled
and zorka will close connection abruply;

* `syslog = yes` - controls whether enable (or disable) syslog support; syslog protocol is enabled by default;

# Configuring Zorka

Zorka exposes its API as modules with functions in a Beanshell interpreter. Zabbix server can call these functions
directly (with slightly different syntax), extension scripts can use them as well (and define their own functions
that will be visible for Zabbix or other monitoring system.

Beanshell scripts are primary way to configure and extend agent. Zorka executes all beanshell scripts from
`$ZORKA_HOME/conf` directory at startup (in alphabetical order). All elements declared in those scripts will
be visible via queries from monitoring servers. User can define his own namespaces, functions. Declaring which
elements should be instrumented (and how) is also possible via `.bsh` scripts.


## Basic API

Zorka exposes standard BeanShell environment (with access to all classes visible from system class loader plus all
Zorka code). There are several library objects visible:

* `zorka` - zorka-specific library functions (logging, JMX access etc., always available);

* `spy` - functions for configuring instrumentation engine (available if spy is enabled);

* `zabbix` - zabbix-specific functions (available if zabbix interface is enabled);

* `nagios` - nagios-specific functions (available if nagios interface is enabled);

All above things are visible Beanshell scripts from `$ZORKA_HOME/conf` directory. Interfaces to monitoring systems
(zabbix, nagios etc.) can call Beanshell functions as well (both built-in and user-defined) but due to their syntax
are typically limited to simple calls. For example:

    zorka.jmx["java","java.lang:type=OperatingSystem","Arch"]

is equivalent to:

    zorka.jmx("java","java.lang:type=OperatingSystem","Arch");


# MBean servers

Zorka can track many mbean servers at once. This is useful for example in JBoss 4/5/6 which have two mbean servers:
platform specific (JVM) and application server specific (JBoss). Each mbean server is available at some name.
Known names:

* `java` - standard plaform mbean server;

* `jboss` - JBoss JMX kernel mbean server;

MBean server name is passed in virtually all functions looking for object names or registering/manipulating
objects/attributes in JMX.

## Examples

With configurable bytecode instrumentation and power of Beanshell, there is a lot of interesting things that can be
done using Zorka. Some of them are described in below examples. For more information about functions used in below
examples see Zorka API Reference section.

### Exposing beanshell function as mbean attribute

With beanshell interface creation feature it is possible to register custom function as MBean attribute. Each time JMX
client asks for a value of this attribute, zorka will execute this function and return its result. Example (getter will
return some text and use some local variable that will persist across calls):

    __mygetter() {
     numCalls = 0;

      get() {
        numCalls++;
        return "This attribute has been read " + numCalls + " times.";
      }

      return this;
    }

    zorka.registerAttr("java", "zorka:type=ZorkaSamples,name=GetterSample",
        "mygetter", (com.jitlogic.zorka.mbeans.ValGetter)__mygetter());

Now you can query it using `zabbix_get` tool:

    # zabbix_get -s 127.0.0.1 -p 10104 \
        -k 'zorka.jmx["java", "zorka:type=ZorkaSamples,name=GetterSample", "mygetter"]'

    This attribute has been read 1 times.
    #

    # zabbix_get -s 127.0.0.1 -p 10104 \
     -k 'zorka.jmx["java", "zorka:type=ZorkaSamples,name=GetterSample", "mygetter"]'
    This attribute has been read 2 times.



### Monitoring response times of Tomcat applications

Instrumenting `invoke()` method in a valve that processes all requests to Tomcat server can be used to monitor HTTP
requests in various ways. Below example will collect statistiscs of HTTP requests by URL:

    spy.add(
      spy.instrument(
        "java", "zorka:type=ZorkaStats,name=TomcatHttpStats", "byUrl", "${1.request.requestURI}")
      .include("org.apache.catalina.core.StandardEngineValve", "invoke"));

URL can be obtained calling `getRequest().getRequestURI()` method on request argument. Note that a convenience
`spy.instrument()` function has been used: it automatically configures spy to fetch and process proper arguments
(see reference documentation for more details).

It is now possible to use `zorka.ls()` function to list all collected statistics:

    zabbix_get -s 127.0.0.1 -p 10104 -k 'zorka.ls["java", \
      "zorka:type=ZorkaStats,name=HttpsStats", "byUrl"]'

Above example will make URIs to all files/servlets visible. In order to make more coarse aggregation, some more
argument processing will be needed. Below example cuts off first segment of URI, so statistics will be aggregated
by application context paths (plus some root-level files/directories if `ROOT.war` is actually accessed):

    spy.add(
      spy.instance()
        .onEnter().withTime().withArguments(1)
          .get(1,1,"request","requestURI")
          .transform(1,1,"^(\\/[^\\/]+).*$","${1}")
        .onReturn().withTime()
        .onError().withTime()
        .onSubmit().timeDiff(0,2,2)
        .toStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byCtx", "${1}", 0, 2)
      .include("org.apache.catalina.core.StandardEngineValve", "invoke")
    );

This time using `spy.instrument()` function is not possible, so spy configuration had to be constructed manually.
It fetches current time and first argument of `invoke()` method at entry point (using `withTime()` and
`withArguments()`), then gets URI string from obtained argument (using `get()`) and cuts off first segment with regular
expression (using `transform()`). It also fetches current time on exit and on error, then it calculates time difference
just before record is submitted to collector (using `timeDiff()`). Note how arguments are ordered at submission time:
it is concatenation of all arguments obtained and processed at entry point and arguments obtained (and processed) at
return or error points - thus we have (entry time, transformed request URI, (error) return time) order, so time has to
be calculated using slots 0 and 2, not 0 and 1. Transformed URI from slot 1 is used as a key by `toStats()` (`${1}`).

Use `zorka.ls()` function to list collected statistics:

    zabbix_get -s 127.0.0.1 -p 10104 -k 'zorka.ls["java", \
	  "zorka:type=ZorkaStats,name=HttpsStats", "byUrl"]'


Another example is collecting statistics sorted out by HTTP status code:

    spy.add(
      spy.instance()
        .onEnter().withTime()
        .onReturn().withTime().withArguments(2).get(1, 1, "response", "status")
        .onSubmit().timeDiff(0,1,1)
        .toStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byStatus", "${2}", 0, 1)
      .include("org.apache.catalina.core.StandardEngineValve", "invoke")
    );

Status code can be obtained by calling `getResponse().getStatus()` on second argument of `invoke()` method and it has
to be executed on method return, not at entry point. Everything else looks similiar to previous example.

While it is possible to enter all three above configurations, it will be inefficient because all three will be executed
separately (thus `invoke()` function will be instrumented with three sets of similiar probes). Thanks to `SpyDefinition`
DSL versatility it is possible to compact all three configurations into one and even add additional statistics.
Example:

    spy.add(
      spy.instance()
        .onEnter().withTime().withArguments(1)
          .get(1,1,"request","requestURI").transform(1,2,"^(\\/[^\\/]+).*$","${1}")
        .onReturn().withTime().withArguments(2).get(1, 1, "response", "status")
        .onSubmit().timeDiff(0,3,3)
        .toStats("java","zorka:type=ZorkaStats,name=HttpStats","byUri","${1}",0,3)
        .toStats("java","zorka:type=ZorkaStats,name=HttpStats","byCtx","${2}",0,3)
        .toStats("java","zorka:type=ZorkaStats,name=HttpStats","byRc","${4}",0,3)
        .toStats("java","zorka:type=ZorkaStats,name=HttpStats","byCtRc",“${4}:${2}",0,3)
        .include("org.apache.catalina.core.StandardEngineValve", "invoke")
      );


Regex transformer (and optionally string formatter) can be used to create arbitrary rules to classify URIs. For example:

    spy.add(
      spy.instance()
        .onEnter().withTime().withArguments(1).get(1,1,"request","requestURI")
          .transform(1,1,"^.*loginform\\.jsp$","loginForm")
          .transform(1,1,"^.*\\.jsp$","java").transform(1,1,"^.*\\.do$","java")
          .transform(1,1,"^.*\\.html$","pages").transform(1,1,"^.*\\.css$","pages")
          .transform(1,1,"^.*\\.gif$","images").transform(1,1,"^.*\\.jpg$","images")
        .onReturn().withTime()
        .onSubmit().timeDiff(0,2,2)
        .toStats("java","zorka:type=ZorkaStats,name=HttpStats","byTag","${1}",0,2)
      .include("org.apache.catalina.core.StandardEngineValve", "invoke")
    );

_TODO_ monitoring unhandled exceptions thrown by tomcat application;


### Monitoring logs

Example below will count all exceptions logged by SLF4J and classify them by exception class name:

    spy.add(
      spy.instance()
        .onEnter().withArguments(2).get(0,0,"class","name")
        .toStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1)
      .include(1, "org.slf4j.impl.JCLLoggerAdapter", "trace", "void", "String", "Throwable")
      .include(1, "org.slf4j.impl.JCLLoggerAdapter", "debug", "void", "String", "Throwable")
      .include(1, "org.slf4j.impl.JCLLoggerAdapter", "info",  "void", "String", "Throwable")
      .include(1, "org.slf4j.impl.JCLLoggerAdapter", "warn",  "void", "String", "Throwable")
      .include(1, "org.slf4j.impl.JCLLoggerAdapter", "error", "void", "String", "Throwable")
    );

    spy.add(
      spy.instance()
        .onEnter().withArguments(3).get(0,0,"class","name")
        .toStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1)
      .include("org.slf4j.impl.SimpleLogger", "log")
    );

    spy.add(
      spy.instance()
        .onEnter().withArguments(4).get(0,0,"class","name")
        .toStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1)
    );

    spy.add(
      spy.instance()
        .onEnter().withArguments(6).get(0,0,"class","name")
        .toStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1)
      .include("org.slf4j.impl.Log4jLoggerAdapter", "log")
    );


_TODO_ monitoring logs by message
_TODO_ Catching deadlocks


# Generic API and Monitoring System Interface API

The API acronym might sound familiar to programmers, yet in this context it should be pronounced as *Agent Programming
Interface* rather as Zorka beanshell scripts are not applications per se.

## General purpose functions

### zorka.jmx()


    zorka.jmx(<mbsName>, <object-name>, <attr1>, <attr2>, ...)

Returns value of given object. Looks in `<mbsName>` mbean server for object named `<object-name>`, fetches `<attr1>`
from it, then fetches `<attr2>` from result of previous fetch etc. Depending on object type, attribute fetch can
involve invoking getter (arbitrary objects), looking for a key (map objects), indexing (lists or arrays). This way user
can access arbitrarily deep into structure of an object available via JMX.

### zorka.jmxLister()

    zorka.jmxLister(<mbsName>, <objectName>)

Creates RankLister instance that will search through all attributes of objects matching `<objectName>` and look for
`ZorkaStats` objects. This is useful for constructing rankings of method call statistics.

### zorka.get()

    zorka.get(<object>, <attr>, <attr>, ...)

Extracts attribute chain from object in the same way `zorka.jmx()` does when it finds mbean.

### zorka.getter()

    zorka.getter(<object>, <attr>, <attr>, ...)

Creates a getter object. Getter object takes some other object and exposes `get()` method that extracts data in the
same way `zorka.get()` does.

### zorka.log*()

    zorka.logDebug(<message>, <args>...)
    zorka.logInfo(<message>, <args>...)
    zorka.logWarning(<message>, <args>...)
    zorka.logError(<message>, <args>...)

This function logs a message to zorka log. Extra arguments `<args>` will be woven into `<message>` using
`String.format()`. If last extra argument is an exception (instance of `java.lang.Throwable`) it won't be used in
string formating - it's stack trace will appear in log instead.

### zorka.mbean()

    zorka.mbean(<mbs>, <mbean-name>)
    zorka.mbean(<mbs>, <mbean-name>, <description>)

Creates and returns a new generic mbean. MBean will be registered under a given name in a given mbean server. Returned
object contains `put()` method that can be used to populate mbean with attributes - either plain values or getter
objects (see `zorka.getter()` function).

### zorka.rate()

    zorka.rate(<mbs>, <object-name>, <attr>, ..., <nom>, <div>, <horizon>)

This function is useful for calculating (windowed) average rates of two attributes of an object (nominator and divider)
in a selected time horizon. It looks for object `<object-name>` in `<mbs>` mbean server, then descends using `<attr>`
arguments (the same way `zorka.jmx()` works), finaly uses `<nom>` and `<div>` parameters as nominator and divider,
calculates deltas of both parameters over `<horizon>` time window and returns `<nom>` delta divided over `<div>` delta.

### zorka.registerAttr()

    zorka.registerAttr(<mbsName>, <beanName>, <attrName>, <obj>)
    zorka.registerAttr(<mbsName>, <beanName>, <attrName>, <obj>, <desc>)

Registers arbitrary object `<obj>` in an `<attrName>` attribute of mbean named `<beanName>` in mbean server `<mbsName>`.
Optionally description can be added using `<desc>` argument.

### zorka.registerMbs()

    zorka.registerMbs(<mbsName>, <mbsObject>)

Register mbean server `<mbsObj>` under `<mbsName>`. This is useful in conjunction with instrumentation for catching
additional mbean servers and registering them. See how registering of JBoss5 mbean server works.

### zorka.reload()

    zorka.reload(<mask>)

Reloads all configuration scripts in `$ZORKA_HOME/conf` directory matching given `<mask>`. Note that this only means
executing scripts once again. It is script implementer responsiblity to make sure that script is able to execute
multiple times (and do reconfiguration properly).

## Zabbix-specific functions

### zabbix.discovery()

    zabbix.discovery(<mbs>, <object-wildcard>, <attr1>, <attr2>, ...)

This function is useful for performing low level discovery for Zabbix. It looks for mbeans matching given wildcard and
for every name found returns a map of attributes (attr1, attr2) mapped onto their values. Map keys are in uppercase
attribute names in zabbix-compatible form, eg. `j2eeServer` maps to `{#J2EESERVER}`. More sophiscated version of this
method will be described in next section, yet this one is sufficient for most cases.

    zabbix.discovery(<mbs>, <object-widcard>,  oattrs, path, attrs)

This is a more sophiscated discovery function that - in addition to listing mbeans themselves - can also dig deeper into
mbean attributes.

# Class and Method Instrumentation API

Instrumentation part of Zorka agent is called Zorka Spy. With version 0.2 a fluent-style configuration API has been
introduced. You can define spy configuration properties in fluent style using `SpyDefinition` object and then submit it
to instrumentation engine. In order to be able to configure instrumentations, you need to understand structure of
instrumentation engine and how events coming from instrumented methods are processed.


`TODO Diagram 1: Zorka Spy processing scheme.`


Zorka Spy will insert probes into certain points of instrumented methods. There are three kinds of points: entry points,
return points and error points (when exception has been thrown). A probe can fetch some data, eg. current time, method
argument, some class (method context) etc. All etched values are packed into a submission record (`SpyRecord` class) and
processed by one of argument processing chains (`ON_ENTER`, `ON_RETURN` or `ON_ERROR` depending of probe type), then
results of all probes from a method call are bound together and  processed by `ON_SUBMIT` chain. All these operations
are performed in method calling thread context (so these processing stages must be thread safe). After that, record is
passed to `ON_COLLECT` chain which is guaranteed to be single threaded. Finally records are dispatched into collectors
(which is also done in a single thread). Collectors can do various things with records: update statistics in some mbean,
call some BSH function, log it to file etc. New collector implementations can be added on the fly - either in Java or
as BeanShell scripts.


Spy definition example:

    collect(record) {
      mbs = record.get(4,0);
      zorka.registerMbs("jboss", mbs);
    }

    sdef = spy.instance().onReturn().withArguments(0)
       .lookFor("org.jboss.mx.server.MBeanServerImpl", "<init>")
       .toBsh("jboss");

    spy.add(sdef);

This is part of Zorka configuration for JBoss 5.x. It intercepts instantiation of `MBeanServerImpl` (at the end of its
constructor) and calls `collect()` function of jboss namespace (everything is declared in jboss namespace).
`SpyDefinition` objects are immutable, so when tinkering with `sdef` multiple times, remember to assign result of last
method call to some variable. Method `spy.instance()` returns empty (unconfigured) object of `SpyDefinition` type that
can be further configured using methods described in next sections.

For more examples see *Examples* section above.

## Spy library functions

    sdef = spy.instance()

Create new (unconfigured) spy configuration. Use methods of returned object to configure it.

    sdef = spy.instrument()

Create partially configured spy configuration. It will fetch current time at the beginning and at the end of
instrumented methods and calculate method execution times

    spy.add(sdef1, sdef2, ...)

Will submit created configurations to instrumentation engine.

## SpyDefinition methods

### Choosing processing stages

Argument fetch and argument processing can be done in one of several points (see Diagram 1). Use the following functions
to choose stage (or probe point):

    sdef = sdef.onEnter();	or	sdef = sdef.on(spy.ON_ENTER);
    sdef = sdef.onExit();		or	sdef = sdef.on(spy.ON_EXIT);
    sdef = sdef.onSubmit();	or	sdef = sdef.on(spy.ON_SUBMIT);
    sdef = sdef.onCollect();	or	sdef = sdef.on(spy.ON_COLLECT);


### Fetching arguments

Data to be fetched by probes can be defined using `withArguments()` method:

    sdef = sdef.withArguments(arg1, arg2, ...);

Arguments can passed as numbers representing argument indexes or special data. For instance methods visible arguments
start with number 1 at number 0 there is reference to object itself (`this`). For static methods arguments start with
number 0.

There are some special indexes that represent other data possible to fetch by instrumentation probes:

* `spy.FETCH_TIME` (-1) - fetch current time;
* `spy.FETCH_RET_VAL` (-2) - fetch return value (this is valid only on return points);
* `spy.FETCH_ERROR` (-3) - fetch exception object (this is valid only on error points);
* `spy.FETCH_THREAD` (-4) - fetch current thread;

It is also possible to fetch classes - just pass strings containing fully qualified class names instread of integers.

Additional notes:

* remember that argument fetch can be done in various points, use `sdef.onXXX()` method to select proper stage;

* when instrumenting constructors, be sure that this reference (if used) can be fetched only at constructor return -
this is because at the beginning of a constructor this points to an uninitialized block of memory that does not
represent any object, so instrumented class won't load and will crash with class verifier error;

There are some convenience methods defined to grab special values:

    sdef = sdef.withTime();
    sdef = sdef.withRetVal();
    sdef = sdef.withError();
    sdef = sdef.withThread();
    sdef = sdef.withClass(className);

### Looking for classes and methods to be instrumented

    sdef = sdef.lookFor(classPattern, methodPattern);

    sdef = sdef.lookFor(access, classPattern, methodPattern, retType, argType1, argType2, ...);

These methods will choose which classes and methods will be instrumented. Both `classPattern` and `methodPattern` can
contain asterisk (`*`) or double asterisk (`**`) which has the same meaning as in Ant or Maven: single asterisk will
choose all classes in given package (eg. `org.jboss.*`), and double asterisk will choose all classes in given package
and all subpackages it contains. For method names single asterisk represents any sequence of characters (double asterisk
makes no sense in method names).

First (short) version of `lookFor()` will choose all public methods matching patterns passed as its arguments, full
version of `lookFor()` allows for much finer control: it is possible to pass access flags (eg. choosing only static
methods etc.) and define return type and types of arguments (as in java - eg. `int`, `void`, `String`,
`com.mycompany.MyClass` etc.).

### Processing and filtering arguments

Argument processor  There is a generic method `withProcessor(obj)` that allows attaching arbitrary argument processors
to spy configuration and some convenience methods attaching predefined processors:

#### Formatting strings

    sdef = sdef.withFormat(dst, formatExpr);

Will evaluate `formatExpr`, substitute values (fetched in the same record) and store result at slot number `dst`.
Format strings look like this:

    "some${0}text${1.some.attr}"

All substitution placeholders are marked with `${....}`. Inside it there is an index of (interesting) variable and
optionally subsequent attributes that allow looking into it (using getters, indexing, keys etc. depending in object
type - as in `zorka.jmx()` function).

#### Filtering records

    sdef = sdef.filter(src, regex);
    sdef = sdef.filterOut(src, regex);

These two methods allow filtering of records. First one will pass only records matching `regex` on argument number `src`.
Second one will do exactly opposite thing.

#### Fetching attributes from arguments

    sdef = sdef.get(src, dst, attr1, attr2, ...);

This will fetch attribute chain `(attr1,attr2,...)` from argument `src` and store result into `dst`.

Calling method of argument object:

    sdef = sdef.callMethod(src, dst, methodName, arg1, arg2, ...);

This will get argument at index `src`, call method named methodName with arguments `(arg1, arg2, ...)` and store result
into `dst`.

#### Calculating time difference between arguments

    sdef = sdef.timeDiff(in1, in2, out);

It will take arguments from `in1` and `in2`, ensure that these are long integers and stores result at `out`.

There is also a generic method for adding custom processors:

    sdef = sdef.withProcessor(processor);

Custom processors must implement `com.jitlogic.zorka.spy.processors.SpyArgProcessor` interface. It is possible to use
Beanshell interface generation feature to integrate simple scripts into argument processing chain, for example:

    __plus2Processor() {
      process(stage, record) {
        record.put(stage, 0, record.get(stage, 0) + 2);
        return record;
      }
    }

    plus2Processor = __plus2Processor();

    sdef = sdef.withProcessor(
      (com.jitlogic.zorka.spy.processors.SpyArgProcessor)plus2Processor);

Additional notes:

* be extremly careful when filtering out objects on `ON_ENTER`, `ON_RETURN` and `ON_ERROR` stages; records passed here
can have counterparts that will be matched later, by filtering one record and passing another it is possible to disrupt
submitter matching them;


### Collecting data

Collectors are final objects receiving (fully processed) and presenting them in some way or performing real actions.
There is a generic method `toCollector()` that allows attaching custom collectors (including beanshell scripts).
There is also a set of convenience methods:

    sdef = sdef.toStats(mbsName, beanName, attrName, keyExpr,
			         tstampField, timeField);

This is collect method call statistics in `ZorkaStats` object visible via JMX as an attribute of some `MBean`.
Method arguments:

* `mbsName` - mbean server name (typically `java` but you can use other mbean servers if accessible, for example `jboss`);

* `beanName` - mbean name (in standard JMX form: `domain:attr1=val1,attr2=val2,...`);

* `attrName` - attribute name as which `ZorkaStats` object will be visible;

* `keyExpr` - value used as key in `ZorkaStats` (which is a kind of dictionary object containing statistics for many
other methods); `tstampField`, `timeField` - indicates indexes at which `timestamp` and execution time will be stored
in incoming records;

It is possible to use some substitution variables in `beanName`, `attrName` and `keyExpr`:

* `${className}` - will be substituted with fully qualified class name;

* `${shortClassName}` - will be substituted with short class name (without package);

* `${methodName}` - will be substituted with method name;

In addition `keyExpr` can contain expressions fetching record arguments `${n.attr1.attr2...}` the same form as in
`withFormat()` method.

It is also possible to attach single-method call statistic directly as mbean attribute:

    sdef = sdef.toStat(mbsName, beanName, attrName, tstampField, timeField);

All parameters are the same as in `toStats()` method (except for `keyExpr` which is missing).

Presenting intercepted values as mbean attributes is possible with `toGetter()` method:

    sdef = sdef.toGetter(mbsName, beanName, attrName, attr1, attr2, ...);

This will present intercepted object as ValGetter attribute. Each time attribute is accessed (eg. via jconsole),
Zorka will fetch value using attribute chain `(attr1, attr2, ...)` as in `zorka.jmx()` call.


