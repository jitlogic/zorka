
# Examples

With configurable bytecode instrumentation and power of Beanshell, there is a lot of interesting things that can be
done using Zorka. Some of them are described in below examples. For more information about functions used in below
examples see Zorka API Reference section.

## Exposing beanshell function as mbean attribute

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



## Monitoring response times of Tomcat applications

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


## Log monitoring



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


## CAS auditing with zorka and syslog

This will be a more complete example: we'll implement audit logging of Jasig CAS that will send audit records to
syslog server.