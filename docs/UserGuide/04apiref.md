
# Zorka API Reference

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

### zorka.fileTrapper()

    zorka.fileTrapper(id)
    zorka.rollingFileTrapper(id, path, maxFiles, maxSize, logExceptions)
    zorka.dailyFileTrapper(id, path, logExceptions)
    zorka.removeFileTrapper(id)

These functions allow creating, accessing and removing file trappers (loggers). File trappers can be used to log arbitrary
things to local files (spy events in particular). There are two types of file trappers: rolling file trapper (that keeps
limited amount of log files of limited size. Rolling trappers can be created using `rollingFileTrapper()` function. Daily
file trappers create files with `.YYYY-MM-DD` extension, so logs are split in daily basis. Daily trappers are created
using `dailyFileTrapper()` function. Both functions register created trappers with `id` tag. Both functions return trapper
created earlier if it has been registered with the same `id`. Using `fileTrapper()` function it is possible to access
trappers created earlier without creating new ones (if none exists). Use `removeFileTrapper()` function to unregister
and dispose file trappers.


## Syslog functions

### syslog.trapper()

    syslog.trapper(id)
    syslog.trapper(id, syslogServer, defaultHost)

Returns syslog logger object (and creates it if object doesn't exist). See logger objects methods to see what it can do.
There are two variants of this method: first one only looks for already configured logger and returns `null` if it finds
nothing. Second one creates logger if necessary. Logger object is registered at syslog lib as `id`, syslog server address
is passed as host name or ip address (with optional port number in `host:port` notation).

### syslog.log()

    syslog.log(id, severity, facility, tag, message)

Logs message to a logger identified by `id`. Integer parameters `severity` and `facility` have to adhere to syslog
standard conventions. Message is tagged using `tag` parameter (typically program name or component name) and log message
is passed via `content` parameter.

Severity and facility codes are defined directly in spy lib. The following severity codes are available: `S_EMERGENCY`,
`S_ALERT`, `S_CRITICAL`, `S_ERROR`, `S_WARNING`, `S_NOTICE`, `S_INFO`, `S_DEBUG`. The following facility codes are valid:
`F_LOCAL0`, `F_LOCAL1`, `F_LOCAL2`, `F_LOCAL3`, `F_LOCAL4`, `F_LOCAL5`, `F_LOCAL6`, `F_LOCAL7`, `F_KERNEL`, `F_USER`,
`F_MAIL`, `F_SYSTEM`, `F_AUTH1`, `F_SYSLOG`, `F_PRINTER`, `F_NETWORK`, `F_UUCP`, `F_CLOCK1`, `F_AUTH2`, `F_FTPD`,
`F_NTPD`, `F_AUDIT`, `F_ALERT`, `F_CLOCK2`.

### syslog.remove()

    syslog.remove(id)

Stops and unregisters logger identified by `id`. Logger's thread will be stopped and UDP socket will be closed. Note
that references to this logger object may be still used somewhere. In such case JVM won't release such object but
all attempts to log using it will be ignored.

### Logger objects

Logger objects can be obtained using `syslog.get()` function and (potentially) can be

#### log()

    <logger-obj>.log(severity, facility, tag, message)
    <logger-obj>.log(severity, facility, hostname, tag, message)

Logs a message. First variant of this method logs message with default host name (which is set while creating object),
in second case user can provide custom host names (it is sometimes useful, yet using one, identifiable hostname per JVM
is generally recommended).

#### stop()

    <logger-obj>.stop()

Calling `stop()` method will stop logger. This means stopping logger's sender thread, closing UDP socket and ignoring
all subsequent `log()` calls.

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

### zabbix.trapper()

    zabbix.trapper(id)
    zabbix.trapper(id, serverAddr, defaultHost)
    zabbix.remove(id)

Returns (and eventually creates) zabbix trapper. Newly created trappers are registered in zabbix library and will be
returned every time `trapper()` function with the same `id` will be called. Parameters:

* `id` - arbitrary string identifying trapper; must be unique in `zabbix` library;

* `serverAddr` - address of zabbix server (plus optional port number in `addr:port` form);

* `defaultHost` - default host name - will be used as host name if no other has been passed;

Last function stops and unregisters trapper registered in zabbix library.

## SNMP functions

Zorka provides SNMP library which currently only provides support for sending SNMP traps. SNMP agent functionality is
not yet implemented albeit it is planned for the future.

### snmp.trapper()

    snmp.trapper(id)
    snmp.trapper(id, snmpAddr, community, agentAddr)
    snmp.trapper(id, snmpAddr, community, agentAddr, protocol)
    snmp.remove(id)

Returns (and eventually creates) SNMP trapper object. Newly created SNMP trappers are registered in `snmp` library and
will be returned every time `trapper()` method with the same `id` will be called. Parameters:

* `id` - arbitrary string that will identify trapper; must be unique in `snmp` library;

* `snmpAddr` - address of SNMP server (and optionally port in `addr:port` form);

* `community` - community ID (as configured in SNMP server);

* `agentAddr` - (advertised) agent IP address;

* `protocol` - SNMP protocol version: `snmp.SNMP_V1` and `snmp.SNMP_V2` are currently supported;

Each created trapper will have its own UDP port and its own thread. Last function removes and stops trapper identified
by `id`.

### snmp.val()

    snmp.val(type, value)

Converts java object into SNMP typed object. The following SNMP types are valid: `snmp.INTEGER`, `snmp.BITSTRING`,
`snmp.OCTETSTRING`, `snmp.NULL`, `snmp.OID`, `snmp.SEQUENCE`, `snmp.IPADDRESS`, `snmp.COUNTER32`, `snmp.GAUGE32`,
`snmp.TIMETICKS`, `snmp.NSAPADDRESS`, `snmp.COUNTER64`, `snmp.UINTEGER32`.

### snmp.oid()

    snmp.oid(template, vals)

Creates an SNMP OID object. Template is an OID string with optional placeholders in standard `${...}` form.
Parameters passed as `vals` are used to fill placeholders.

### snmp.bind()

    snmp.bind(slot, type, oidSuffix)

Creates spy record to trap variable binding object. It will map object from `slot` in `ON_COLLECT` buffer of spy record
to a SNMP object of `type` tagged with `oidSuffix`. OID suffix will be added to base OID in traps sent by `SnmpCollector`
component of Zorka Spy. See `sdef.toSnmp()` description below for more information about bindings.

## Data normalization

There are cases when intercepted data has to be normalized in some way. Removing data from intercepted SQL queries is
one example but there are many types of data that should be stripped or simplified in some way. Data normalization
framework functions are available via `normalizers` library. Objects created with those functions can be attached to
spy processing chains using `normalize()` method of spy definition objects.

There are several predefined normalization modes, passed to normalizer constructors as `flags` parameter:

* `normalizers.NORM_MIN` - minimal normalizer (cut off white spaces and illegal tokens, normalize white spaces, symbols
and keywords, leave values (literals) unchanged (and visible);

* `normalizers.NORM_STD` - everything `NORM_MIN` does plus remove literals and replace them with `?` placeholders.

### normalizers.sql()

    normalizers.sql(dialect, flags)

Creates SQL normalizer that supports various SQL dialects and similiar languages (eg. hibernate HQL or JPA EQL). The
following dialects are currently supported:

* `normalizers.DIALECT_SQL99` - ANSI SQL-99;

Use one of `normalizers.NORM_*` constants as `flags` parameter.


### normalizers.ldap()

    normalizers.ldap(flags)

Creates LDAP normalizer that processes LDAP search queries. Use one of `normalizers.NORM_*` constants as `flags` parameter.

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
* `spy.FETCH_NULL` (-5) - fetch null constant (useful in some cases);

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
    sdef = sdef.withNull();

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

#### Storing data across method calls using ThreadLocal

Sometimes it is useful to use data grabbed from some method and use it along with other method further down call stack.
The following methods can be used with thread local objects:

    sdef = sdef.set(src, threadLocal)
    sdef = sdef.get(dst, threadLocal)
    sdef = sdef.remove(threadLocal)

Thread local object has to be declared in BSH script and is passed as `threadLocal` argument to each of these methods.
Method `set()` stores value from slot `src` in `threadLocal`. Method `get()` reads value from `threadLocal` and stores
it in slot `dst`. Last method clears `threadLocal`, so stored object can be garbage collected (granted there are no more
references pointing to it).

#### Filtering data using comparator

    sdef = sdef.ifSlotCmp(a, op, b)
    sdef = sdef.ifValueCmp(a, op, v)

These two methods can be used to filter out unwanted records. First one compares values from two slots of a record,
second one compares value from record slot with some constant. Arguments `a` and `b` are slot numbers, `v` is a constant
value and `op` is one of operators: `spy.GT`, `spy.GE`, `spy.EQ`, `spy.LE`, `spy.LT`, `spy.NE`. Both functions will
coerce input values to proper types in order to make comparison possible. Floating point values (`float` or `double`)
are compared with `0.001` accuracy. For example:

* `ifSlotCmp(0, spy.GT, 1)` will pass only records for whose value from slot 0 is greater than value from slot 1;

* `ifValueCmp(0, spy.LT, 42)` will pass only records for whose value from slot 0 is less than 42;

#### Normalizing data (eg. queries)

    sdef = sdef.normalize(src, dst, normalizer);

Plugs a normalizer into spy processing chain. Spy will take value from `src` slot, pass it through `normalizer` object
and store result in `dst` slot.

#### Using custom processors

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
There is also a set of convenience methods useful to configure collector quickly:

#### Collecting to Zorka Stats

    sdef = sdef.toStats(mbsName, beanName, attrName, keyExpr, tstampField, timeField);

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

* `${packageName}` - package name of instrumented class;

* `${methodName}` - will be substituted with method name;

In addition `keyExpr` can contain expressions fetching record arguments `${n.attr1.attr2...}` the same form as in
`withFormat()` method.

It is also possible to attach single-method call statistic directly as mbean attribute:

    sdef = sdef.toStat(mbsName, beanName, attrName, tstampField, timeField);

All parameters are the same as in `toStats()` method (except for `keyExpr` which is missing).

#### Intercepting and presenting objects via Zorka Getter

Presenting intercepted values as mbean attributes is possible with `toGetter()` method:

    sdef = sdef.toGetter(mbsName, beanName, attrName, attr1, attr2, ...);

This will present intercepted object as ValGetter attribute. Each time attribute is accessed (eg. via jconsole),
Zorka will fetch value using attribute chain `(attr1, attr2, ...)` as in `zorka.jmx()` call.

#### Logging collected events via Syslog

    sdef = sdef.toSyslog(trapper, expr, severity, facility, hostname, tag)

Parameter `trapper` must be a reference to trapper object obtained using `syslog.trapper()`. Parameter `expr` is message
template (analogous to `keyExpr` in other collectors). Remaining parameters - `severity`, `facility`, `hostname` and
`tag` work in the same way as in `syslog.log()` method.

#### Sending collected events as SNMP traps

    sdef = sdef.toSnmp(trapper, oid, spcode, bindings)

Parameter `trapper` must be a reference to trapper object obtained using `snmp.trapper()` function. Traps will have their
`enterprise-oid` field set to `oid` and all variables will have their keys starting with `oid`. Traps will be of
`enterpriseSpecific` (6) type and specific code will be set to `spcode`.

#### Sending collected events to Zabbix

    sdef = sdef.toZabbix(trapper, expr, key)
    sdef = sdef.toZabbix(trapper, expr, host, key)

Parameter `trapper` must be a reference to zabbix trapper obtained using `zabbix.get()` function. Parameter `expr` is
message template (analogous to `keyExpr` in other collectors). Parametry `key' refers to zabbix item key that will be
populated. Item must be of proper type (text or number depending on data that is submitted).

#### Sending collected records to log file

    sdef = sdef.toFile(trapper, expr, logLevel)

Parameter `trapper` must be a reference to file trapper obtained using `zorka.fileTrapper()`, `zorka.dailyFileTrapper()`
or `zorka.rollingFileTrapper()`. Second parameter `expr` contains template string used to create log messages. Parameter
`logLevel` must be one of: `zorka.TRACE`, `zorka.DEBUG`, `zorka.INFO`, `zorka.WARN`, `zorka.ERROR` and `zorka.FATAL`.




