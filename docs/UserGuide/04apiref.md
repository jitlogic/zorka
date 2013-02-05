
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

### Configuration access

    zorka.hasCfg(key)

    zorka.boolCfg(key)
    zorka.boolCfg(key, defval)

    zorka.intCfg(key)
    zorka.intCfg(key, defval)

    zorka.longCfg(key)
    zorka.longCfg(key, defval)

    zorka.stringCfg(key)
    zorka.stringCfg(key, defval)

    zorka.listCfg(key, defv1, defv2, ...)

These are convenience functions for accessing configuration settings from `zorka.properties` file. Adminstrator can
place arbitrary configuration settings into `zorka.properties` and then use them in BSH scripts. First function
`hasCfg()` checks if given setting exists in `zorka.properties` and is contains non-empty string - returns true
if so, or false if not. There are three sets of functions for parsing boolean, integer and long and string settings.
Those functions return parsed values or supplied values if valid settings in `zorka.properties` do not exist or are
not parsable. Boolean parsing `boolCfg()` functions recognize `true`/`false` and `yes`/`no` values (case-insensitive).

List parsing function `listCfg()` parses string containing comma-separated values and returns it as list of strings
containing (trimmed) values.

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

    zabbix.discovery(query1, query2, ...)

This is a more sophiscated discovery function that - in addition to listing mbeans themselves - can also dig deeper into
mbean attributes. It uses JMX query objects that can be defined by `zorka.query()` function.


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

## Data normalization functions

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


## Spy library functions

    sdef = spy.instance()

Create new (unconfigured) spy configuration. Use methods of returned object to configure it.

    sdef = spy.instrument()

Create partially configured spy configuration. It will fetch current time at the beginning and at the end of
instrumented methods and calculate method execution times

    spy.add(sdef1, sdef2, ...)

Will submit created configurations to instrumentation engine.

### Fetching arguments

A set of functions has been defined to create spy probes that will can be injected into instrumented methods. All probes
have `key` parameter that is used to store obtained values in fetch probes and some functions have additiona parameters.

#### spy.fetchArg()

    spy.fetchArg(key, num)

Fetches `num`-th method argument. For instance method visible arguments start with 1 and argument number 0 contains
reference to object instance on behalf of which method has been called (`this`). For static methods arguments start with
0. Note that when instrumenting constructors, fetching reference to `this` is illegal at the beginning of constructor.
You have to to this at return point of constructor.

#### spy.fetchClass()

    spy.fetchClass(key, name)

Fetches class named `name` in the context of instrumented method (that is, with class loader of its parent class).
Returns `java.lang.Class` object.

#### spy.fetchError()

    spy.fetchError(key)

Fetches exception thrown out of method. This can be used only in error handling path (`.onError(...)`) of spy def.

#### spy.fetchRetVal()

    spy.fetchRetVal(key)

Fetches return value of a method. This can be used only at return points (`.onReturn(...)`) of spy def.

#### spy.fetchThread()

    spy.fetchThread(key)

Fetches thread executing method code. Returns `java.lang.Thread` object.

#### spy.fetchTime()

    spy.fetchTime(key)

Fetches current time (`System.nanoTime()` result - nanoseconds since Epoch).

### Matching methods to be instrumented

Matchers that are passed as arguments to `include()` method of spy definition objects can be created using `spy.byXXX()`
functions. There are several functions that can create various types of matchers.

#### spy.byMethod()

    spy.byMethod(classPattern, methodPattern)
    spy.byMethod(access, classPattern, methodPattern, returnType, String...argTypes)

These matchers will choose which classes and methods will be instrumented. Both `classPattern` and `methodPattern` can
contain asterisk (`*`) or double asterisk (`**`) which has the same meaning as in Ant or Maven: single asterisk will
choose all classes in given package (eg. `org.jboss.*`), and double asterisk will choose all classes in given package
and all subpackages it contains. For method names single asterisk represents any sequence of characters (double asterisk
makes no sense in method names).

First (short) version of `byMethod()` will choose all public methods matching patterns passed as its arguments, full
version of `byMethod()` allows for much finer control: it is possible to pass access flags (eg. choosing only static
methods etc.) and define return type and types of arguments (as in java - eg. `int`, `void`, `String`,
`com.mycompany.MyClass` etc.).

#### spy.byClassAnnotation()

    spy.byClassAnnotation(classAnnotation)
    spy.byClassAnnotation(classAnnotation, methodPattern)
    spy.byMethodAnnotation(classPattern, methodAnnotation)
    spy.byClassMethodAnnotation(classAnnotation, methodAnnotation)

These matchers will choose classes that are annotated by `classAnnotation` and methods matching `methodPattern` or
all methods except constructors if method pattern has not been specified. Third and fourth variant allows matching
also by method annotations. These matchers are useful in some cases, for example when looking for all EJB beans of
certain type.


### Processing and filtering arguments

Argument processor  There is a generic method `withProcessor(obj)` that allows attaching arbitrary argument processors
to spy configuration and some convenience methods attaching predefined processors:

#### Formatting strings

    spy.format(dst, formatExpr);

Will evaluate `formatExpr`, substitute values (fetched in the same record) and store result at slot number `dst`.
Format strings look like this:

    "some${0}text${1.some.attr}and${E2.other.attr}"

All substitution placeholders are marked with `${....}`. Inside it there is an index of (interesting) variable and
optionally subsequent attributes that allow looking into it (using getters, indexing, keys etc. depending in object
type - as in `zorka.jmx()` function).

#### Filtering records

    spy.regexFilter(src, regex);
    spu.regexFilter(src, regex. filterOut);

These two methods allow filtering of records. First one will pass only records matching `regex` on argument number `src`.
Second one will do exactly opposite thing if `filterOut` argument is `true` or behave as first one if `false`.

#### Fetching attributes from arguments

    spy.get(src, dst, attr1, attr2, ...);

This will fetch attribute chain `(attr1,attr2,...)` from `src` slot of passing spy records and store result into `dst`
slot of spy records.

#### Calling method of argument object

    spy.call(src, dst, methodName, arg1, arg2, ...);

This will get argument at index `src`, call method named methodName with arguments `(arg1, arg2, ...)` and store result
into `dst`.

#### Calculating time difference between arguments

    spy.tdiff(in1, in2, out);

It will take arguments from `in1` and `in2`, ensure that these are long integers and stores result at `out`.

#### Storing data across method calls using ThreadLocal

Sometimes it is useful to use data grabbed from some method and use it along with other method further down call stack.
The following methods can be used with thread local objects:

    spy.get(dst, threadLocal, path...)
    spy.put(src, threadLocal)
    spy.remove(threadLocal)

Thread local object has to be declared in BSH script and is passed as `threadLocal` argument to each of these methods.
Method `put()` stores value from slot `src` in `threadLocal`. Method `get()` reads value from `threadLocal` and stores
it in slot `dst`. Last method clears `threadLocal`, so stored object can be garbage collected (granted there are no more
references pointing to it).

#### Filtering data using comparator

    spy.ifSlotCmp(a, op, b)
    spy.ifValueCmp(a, op, v)

These two methods can be used to filter out unwanted records. First one compares values from two slots of a record,
second one compares value from record slot with some constant. Arguments `a` and `b` are slot numbers, `v` is a constant
value and `op` is one of operators: `spy.GT`, `spy.GE`, `spy.EQ`, `spy.LE`, `spy.LT`, `spy.NE`. Both functions will
coerce input values to proper types in order to make comparison possible. Floating point values (`float` or `double`)
are compared with `0.001` accuracy. For example:

* `ifSlotCmp(0, spy.GT, 1)` will pass only records for whose value from slot 0 is greater than value from slot 1;

* `ifValueCmp(0, spy.LT, 42)` will pass only records for whose value from slot 0 is less than 42;

#### Normalizing data (eg. queries)

    spy.normalize(src, dst, normalizer)

Plugs a normalizer into spy processing chain. Spy will take value from `src` slot, pass it through `normalizer` object
and store result in `dst` slot.

#### Using custom processors

Custom processors must implement `com.jitlogic.zorka.spy.processors.SpyArgProcessor` interface. It is possible to use
Beanshell interface generation feature to integrate simple scripts into argument processing chain, for example:

    __plus2Processor() {
      process(stage, record) {
        record.put(stage, 0, record.get(stage, 0) + 2);
        return record;
      }
    }

    plus2Processor = __plus2Processor();

    sdef = sdef.onEnter(
      (com.jitlogic.zorka.agent.spy.SpyProcessor)plus2Processor);

Be extremly careful when filtering out objects on `ON_ENTER`, `ON_RETURN` and `ON_ERROR` stages; records passed here
can have counterparts that will be matched later, by filtering one record and passing another it is possible to disrupt
submitter matching them;


### Collecting data

Collectors are final objects receiving (fully processed) and presenting them in some way or performing real actions.
There is a generic method `toCollector()` that allows attaching custom collectors (including beanshell scripts).
There is also a set of convenience methods useful to configure collector quickly:

#### Collecting to Zorka Stats

    spy.zorkaStats(mbsName, beanName, attrName, keyExpr, tstampField, timeField)

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

#### Intercepting and presenting objects via Zorka Getter

Presenting intercepted values as mbean attributes is possible with `toGetter()` method:

    spy.getterCollector(mbsName, beanName, attrName, attr1, attr2, ...)

This will present intercepted object as ValGetter attribute. Each time attribute is accessed (eg. via jconsole),
Zorka will fetch value using attribute chain `(attr1, attr2, ...)` as in `zorka.jmx()` call.

#### Sending collected events as SNMP traps

    spy.snmpCollector(trapper, oid, spcode, bindings)

Parameter `trapper` must be a reference to trapper object obtained using `snmp.trapper()` function. Traps will have their
`enterprise-oid` field set to `oid` and all variables will have their keys starting with `oid`. Traps will be of
`enterpriseSpecific` (6) type and specific code will be set to `spcode`.

#### Using trappers with spy

    spy.trapperCollector(trapper, logLevel, tagExpr, msgExpr, errExpr, errField)

This function creates collector that will send messages via trapper. Several trapper types are available:

* file trappers - trappers that log messages to log file;
* syslog trappers - trappers that send syslog messages;
* zabbix trappers - trappers that send traps directly to zabbix;

Arguments:

* `trapper` - configured and started trapper object;
* `logLevel` - log level (ZorkaLogLevel constant);
* `tagExpr` - log tag;
* `msgExpr` - log message (if call succeeds);
* `errExpr` - log message (if error occurs);
* `errField` - spy record field that contains intercepted exception object;

#### Passing records to another processing chain

    spy.sdefCollector(sdef)

Passes records to another processing chain. Processning chain object reference will be passed as `sdef`.

#### Passing records to log processing chains

    spy.logFormatCollector(processor, level, msgTemplate)
    spy.logFormatCollector(processor, level, msgTemplate, classTempalte, methodTemplate, excTemplate)

Creates log formatting collector - a collector which constructs log records from various bits of information passed by
spy records and then passes is to a log processing chain referenced by `processor`.

    spy.logAdapterCollector(processor, src)

This function returns collector that will automatically convert other types of log records (eg. log4j, jdk logger,
jboss 7 logger etc.) and pass them to log processing chain.

### Collecting method traces


#### Selecting classes and method for trace

    tracer.include(matcher, matcher, ....)
    tracer.exclude(matcher, matcher, ....)

First function adds matcher that will include classes (methods), second adds matcher that will exclude classes (or
methods). Note that added matcher are evaluated in order they've been added, so you may consider adding general
exclusions first and add "everything" after that.

#### Marking trace beginning

    tracer.begin(label)
    tracer.begin(label, minExecutionTime)
    tracer.begin(label, minExecutionTime, flags)

This processor marks beginning of a trace. Tracer will start collecting method call data when it application enters to
an instrumented method that has `tracer.begin()` in its processing chain and will submit collected data at method exit.

Traces are submitted only when execution took more than predefined time. This minimum time can be passed as an argument
of `tracer.begin()` or some system-wide default value can be used. Third variant of this functions allows for setting
additional flags to trace (see `tracer.flags()` for more details).

#### Adding attributes

    tracer.attr(fieldName, attrName)

This processor will add value from given spy record field as a named attribute to a trace. It can be invoked at any
method not only trace beginning. Attributes will be attached to this method's trace record (so the same attribute can
appear at many records in the same trace).

#### Marking trace flags

    tracer.flags(flags)
    tracer.flags(srcField, flags)

This function sets flags altering tracer behavior. First variant sets flags unconditionally. Second variant sets
flags only if given field of spy record contains non-null value. Only currently started trace is marked.
The following flags are avaiable:

* `tracer.ALWAYS_SUBMIT` - forces submission of current trace regardless of other conditions;

* `tracer.ALL_METHODS` - records all method calls of current trace regardless of other conditions;

* `tracer.DROP_INTERIM` - drops interim methods if their execution time is too short;

#### Configuring tracer output

    tracer.toFile(path, maxFiles, maxSize)
    tracer.output(output)

First function creates trace file writer object that can be installed into tracer using second function.

#### Global tracer options

    tracer.setTracerMinMethodTime(nanoseconds);
    tracer.setTracerMinTraceTime(milliseconds);
    tracer.setTracerMaxTraceRecords(nrecords);
    tracer.setDefaultTraceFlags(flags);

First function sets default minimum execution time for a method to be included into trace. Note that due to required
resolution, passed time parameter is in nanoseconds (not milliseconds as usual). Second function sets default minimum
execution time for a trace to be further processed after completion. Third function sets limit for a number of records
included in a single trace. This limit is necessary to prevent tracer from overruning host memory in cases there traced
method calls lots and lots of other traced methods (and all become logged for some reason).


## Collecting performance metrics

Functions for performance metrics are available via `perfmon` library. There are several aspects of configuring it:

* what objects are to be scanned and collected;

* how collected metrics should be preprocessed and described;

* how often metrics should be collected;


### Querying JMX object graph

JMX object graph query consists of JMX object name mask, list of object name attributes to be fetched and sets of
rules defining how to traverse fetched objects. Query result is a list of objects containing finally reached object
(or value) and map of all attributes (explicitly) fetched while traversing object graph. Query definition objects
are immutable but offer some methods that create altered variants.

Simplest query object can be created using `zorka.query()` function:

    zorka.query(mbsName, objectName, attr1, attr2, ....)

Query rule that will descend into selected attribute of a method (and optionally remember its name) can be added using
`get()` method:

     query = query.get(attr)
     query = query.get(attr, name)

First variant of this method will simply descend into objects, second one will also add attribute name to query result.
When interpreting `get()` rule, agent will use `ObjectInspector.get()` method to descend.

Query rule that will list object attributes and descend into matching ones can be defined using `list()` method:

    query = query.list(regex)
    query = query.list(regex, name)

When interpreting `list()` fule, agent will use `ObjectInspector.list()` method to list all attributes and then
`ObjectInspector.get()` method to descend into matching attributes. For each matching attribute, separate result
is created and (optionally) attribute name is stored.

Metric templates are used by permon scanners and can be assigned to queries using `metric()` method:

    query = query.metric(template)

Created (and configured) query objects can be supplied to metrics scanners or discovery functions (eg. for zabbix).

### Defining metric templates

Metric templates define how metrics should be preprocessed and how should metrics present themselves. There are several
kinds of metrics:

    perfmon.metric(name, units)

Creates metric that will simply pass obtained values without preprocessing. Metric name will be visible in trace viewer
and can contain macros referring to attributes attached to query results (in usual form `${attributeName}`). Second
parameters describes measurement units as displayed in trace viewer.

    perfmon.delta(name, units)

Creates metric that will calculate simple delta of obtained values. Supplied parameters have the same meaning as above.

    perfmon.timedDelta(name, units)

Creates metric that will calculate delta per second of obtained values. Supplied parameters have the same meaning as
above.

    perfmon.rate(name, units, nom, div)

This metric tracks two attributes of obtained object at once and calculates rate as delta of `nom` attribute divided
by delta of `div` attribute.

Metric template objects created by above functions are immutable objects that can be altered in the following ways:

    template = template.multiply(multiplier)

This causes metric results to be multiplied by given multiplier.

    template = template.dynamicAttrs(attr, attr, ...)

Marks some query result attributes as dynamic. Dynamic attributes are passed with each metric instance and do not impose
stress on symbol registry nor metric registry.

### Defining metric scanners

    scanner = perfmon.scanner(name, query1, query2, ...)

Creates scanner object that will periodically execute all queries and process their results accordingly. All resulting
metrics are grouped under single scanner name. Note that queries that have no metric templates assigned will be ignored.

Scanner objects implement `Runnable` interface and can be supplied to all kinds of schedulers of executors (eg. using
`zorka.schedule()` function).
