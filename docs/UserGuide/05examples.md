
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
        .onEnter(spy.FETCH_TIME, 1,
          spy.get(1,1,"request","requestURI"),
          spy.transform(1,1,"^(\\/[^\\/]+).*$","${1}"))
        .onReturn(spy.FETCH_TIME)
        .onSubmit(
          spy.tdiff("E0","R0","S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byCtx", "${E1}", "R0", "S0"))
      .include(
        spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
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
        .onEnter(spy.FETCH_TIME)
        .onReturn(spy.FETCH_TIME, 2,
          spy.get(1, 1, "response", "status"))
        .onSubmit(
          spy.tdiff("E0","R0","S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byStatus", "${E1}", "R0", "S0"))
      .include(
        spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
    );

Status code can be obtained by calling `getResponse().getStatus()` on second argument of `invoke()` method and it has
to be executed on method return, not at entry point. Everything else looks similiar to previous example.

While it is possible to enter all three above configurations, it will be inefficient because all three will be executed
separately (thus `invoke()` function will be instrumented with three sets of similiar probes). Thanks to `SpyDefinition`
DSL versatility it is possible to compact all three configurations into one and even add additional statistics.
Example:

    spy.add(
      spy.instance()
        .onEnter(spy.FETCH_TIME, 1,
          spy.get(1,1,"request","requestURI"),
          spy.transform(1,2,"^(\\/[^\\/]+).*$","${1}"))
        .onReturn(spy.FETCH_TIME, 2,
          spy.get(1, 1, "response", "status"))
        .onSubmit(
          spy.tdiff("E0","R0","S1"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byUri", "${E1}", "R0", "S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byCtx", "${E2}", "R0", "S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byStatus", "${R1}", "R0", "S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byCtxStatus", "${E2}|${R1}", "R0", "S0"))
      .include(
          spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
    );


Regex transformer (and optionally string formatter) can be used to create arbitrary rules to classify URIs. For example:

    spy.add(
      spy.instance()
        .onEnter(spy.FETCH_TIME, 1,
          spy.get(1,1,"request","requestURI"),
          spy.transform("E1","E1","^.*\\.jsp$","javaApps"),
          spy.transform("E1","E1","^.*\\.do$","javaApps"),
          spy.transform("E1","E1","^.*\\.html$","staticPages"),
          spy.transform("E1","E1","^.*\\.css$","staticPages"),
          spy.transform("E1","E1","^.*\\.gif$","images"),
          spy.transform("E1","E1","^.*\\.jpg$","images"),
          spy.transform("E1","E1","^.*\\.png$","images"),
          spy.transform("E1","E1","^.*\\.ico$","images"),
          spy.transform("E1","E1","^.*loginform\\.jsp$","loginForm"),
          spy.transform("E1","E1","^\\/.*", "other"))
        .onReturn(spy.FETCH_TIME)
        .onSubmit(
          spy.tdiff("E0","R0","S0"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=HttpStats", "byTag", "${E1}", "E0", "S0"))
      .include(
          spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
    );

_TODO_ monitoring unhandled exceptions thrown by tomcat application;


## Log monitoring



Example below will count all exceptions logged by SLF4J and classify them by exception class name:

    spy.add(
      spy.instance()
        .onEnter(2,
          spy.get(0,0,"class","name"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1))
      .include(
        spy.byMethod(1, "org.slf4j.impl.JCLLoggerAdapter", "trace", "void", "String", "Throwable"),
        spy.byMethod(1, "org.slf4j.impl.JCLLoggerAdapter", "debug", "void", "String", "Throwable"),
        spy.byMethod(1, "org.slf4j.impl.JCLLoggerAdapter", "info",  "void", "String", "Throwable"),
        spy.byMethod(1, "org.slf4j.impl.JCLLoggerAdapter", "warn",  "void", "String", "Throwable"),
        spy.byMethod(1, "org.slf4j.impl.JCLLoggerAdapter", "error", "void", "String", "Throwable")));

    spy.add(
      spy.instance()
        .onEnter(3,
          spy.get(0,0,"class","name"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1))
      .include(
        spy.byMethod("org.slf4j.impl.SimpleLogger", "log")));

    spy.add(
        spy.instance()
        .onEnter(4,
          spy.get(0,0,"class","name"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1)));

    spy.add(
      spy.instance()
        .onEnter(6,
          spy.get(0,0,"class","name"),
          spy.zorkaStats("java", "zorka:type=ZorkaStats,name=LoggedErrors", "byException", "${0}", -1, -1))
      .include(
        spy.byMethod("org.slf4j.impl.Log4jLoggerAdapter", "log")));


_TODO_ monitoring logs by message
_TODO_ Catching deadlocks


## CAS auditing with zorka and syslog

This will be a more complete example. We'll re-implement audit logging of Jasig CAS that will send audit records to
syslog server. CAS already has some audit capability implemented using `inspektr` library that stores audit data in
application logs. CAS auditing configuration will be enclosed in its own namespace to avoid polluting default (global)
namespace:

    __cas() {

      severity = syslog.S_INFO;
      facility = syslog.F_LOCAL5;
      logger = syslog.trapper("audit", "127.0.0.1", "cas");

      request = new ThreadLocal();

      // Intercept request dispatcher and store request object for later use
      spy.add(spy.instance()
        .onEnter(1, spy.tlSet(0, request))
        .onReturn(1, spy.tlRemove(request))
        .onError(1, spy.tlRemove(request))
        .include(
          spy.byMethod("org.jasig.cas.web.init.SafeDispatcherServlet", "service")));


      // Authentication attempts
      spy.add(spy.instance()
        .onReturn(1,
          spy.get("S0", "E0"),
          spy.format("S1","AUTHENTICATION_SUCCESS"),
          spy.tlGet("S2", request))
        .onError(1,
          spy.get("S0", "E0"),
          spy.format("S1","AUTHENTICATION_FAILED"),
          spy.tlGet("S2", request))
        .onSubmit(
          spy.syslogCollector(logger, "remote=${2.remoteAddr} local=${2.localAddr} action=${1} who=${0} what=${0}", severity, facility, "cas", "cas"))
        .include(
          spy.byMethod("org.jasig.cas.authentication.AbstractAuthenticationManager", "authenticate")));


        // ... (other methods instrumented) ...

        return this;
    }

    cas = __cas();

Aside of two constants (used later for convenience), there is a logger defined (sending logs to localhost) and one thread
local object in `request` variable. It will be used to store servlet requests objects that are used to log client IP
addresses along with audit events. In order to intercept request object, `SafeDispatcherServlet.service()` method is
instrumented. It is main entry point to CAS web tier code. Object reference is stored with `set(slot, threadLocal)`
method and cleared up with `remove(threadLocal)`. It is not necessary to clean up request object when exiting `service()`
method, yet leaving it incurs unnecessary burden on VM garbage collector as request object is stored for (potentially)
indefinite time. For all audited (instrumented) methods we use `get(slot, threadLocal)` method to retrieve request object
to a specific slot, so we can access local and remote addresses using `${slot.remoteAddr}` and `${slot.localAddr}` in
format strings.

Sample logged records (remember to configure syslog to receive logs via UDP and store it in dedicated file):

    Dec 1 10:52:00 cas cas remote=127.0.0.1 local=127.0.0.1 action=AUTHENTICATION_FAILED who=[username: test] what=[username: test]
    Dec 1 10:52:00 cas cas remote=127.0.0.1 local=127.0.0.1 action=TICKET_GRANTING_TICKET_NOT_CREATED who=[username: test]
    what=org.jasig.cas.ticket.TicketCreationException: error.authentication.credentials.bad
    Dec 1 10:52:06 cas cas remote=127.0.0.1 local=127.0.0.1 action=AUTHENTICATION_SUCCESS who=[username: test] what=[username: test]
    Dec 1 10:52:06 cas cas remote=127.0.0.1 local=127.0.0.1 action=TICKET_GRANTING_TICKET_CREATED who=[username: test]
    what=TGT-1-qOggyd15UV6vFuyUncaeEBH1MBbdRY0kCCJKz3YDuOBOGw2f1E-cas01.example.org
    Dec 1 11:08:07 cas cas remote=127.0.0.1 local=127.0.0.1 action=AUTHENTICATION_SUCCESS who=[username: test] what=[username: test]
    Dec 1 11:08:07 cas cas remote=127.0.0.1 local=127.0.0.1 action=TICKET_GRANTING_TICKET_CREATED who=[username: test]
    what=TGT-1-ULOCJ1WsCUPRBykByzvbjnkk1GbeocPilEOQm26N34cugt7KSn-cas01.example.org


Eight methods have been instrumented, only one is shown above. See `configs/samples/scripts/auditcas1.bsh` for full script.
The `auditcas2.bsh` file contains the same script refactored in a way that it can send traps to syslog and zabbix.

Third variant of this script - `auditcas3.bsh` - sends SNMP traps instead of text logs. It allows us to send structured
messages instead of formated text. Each field of audit message has its own key-value pair added to SNMP trap. Actions and
result statuses are encoded as integers:

    // Action codes (sent as oid.3)
    AUTHENTICATION = 1;
    TGT_CREATED = 2;
    TGT_DESTROYED = 3;
    SVT_GRANTED = 4;
    SVT_VALIDATED = 5;
    PROXY_GRANTED = 6;
    SVC_SAVED = 7;
    SVC_DELETED = 8;

    // Result codes (send as oid.4)
    FAILURE = 0;
    SUCCESS = 1;

Two global variables actually matter (enclosed in `cas` namespace as in previous examples):

    request = new ThreadLocal();
    trapper = snmp.trapper("audit", "127.0.0.1", "public", "127.0.0.1");

As most of fields are the same for all messages a convention has been assumed in collected records and quite a big chunk
of work has been moved to common `audit()` function:

    audit(sdef, action) {

      // SUCCESS path ...
      sdef = sdef.onReturn(
          spy.put("S2", action),
          spy.put("S3", SUCCESS),
          spy.tlGet("S4", request, "remoteAddr"),
          spy.tlGet("S5", request, "localAddr"));

      // FAILURE path ...
      sdef = sdef.onError(
          spy.put("S2", action),
          spy.put("S3", FAILURE),
          spy.tlGet("S4", request, "remoteAddr"),
          spy.tlGet("S5", request, "localAddr"));

      // Configure SNMP trap collector
      sdef = sdef.onSubmit(spy.snmpCollector(trapper, oid, 0,
         snmp.bind(0, snmp.OCTETSTRING, "1"), // WHO  (String)
         snmp.bind(1, snmp.OCTETSTRING, "2"), // WHAT (String)
         snmp.bind(2, snmp.INTEGER, "3"),     // ACTION (int)
         snmp.bind(3, snmp.INTEGER, "4"),     // RESULT (int)
         snmp.bind(4, snmp.IPADDRESS, "5"),   // REMOTE (ip address)
         snmp.bind(5, snmp.IPADDRESS, "6")));  // LOCAL (ip address)

      spy.add(sdef);
    }

So, most fetched fields are actually configured in `audit()` function. Two remaining ones are `WHO` and `WHAT` stored at
slots `0` and `1`. These are configured when instrumenting actual methods, for example:

    // Authentication attempts
    sdefAuth = spy.instance()
      .onReturn().withArguments(1,1)
      .onError().withArguments(1,1)
      .include("org.jasig.cas.authentication.AbstractAuthenticationManager", "authenticate");
    audit(sdefAuth, AUTHENTICATION);

All remaining methods are similiarly configured. See `auditcas3.bsh` for entire script. Results can be viewed using
`snmptrapd` program or `tcpdump`:

    # tcpdump -ni lo -s 1024 udp port 162
    listening on lo, link-type EN10MB (Ethernet), capture size 1024 bytes
    23:50:51.224454 IP 127.0.0.1.35370 > 127.0.0.1.snmptrap:  Trap(156)  .1.3.6.1.2.1.1.1 127.0.0.1 enterpriseSpecific
    s=0 2296238946 .1.3.6.1.2.1.1.1.1="[username: test]" .1.3.6.1.2.1.1.1.2="[username: test]" .1.3.6.1.2.1.1.1.3=1
    .1.3.6.1.2.1.1.1.4=1 .1.3.6.1.2.1.1.1.5=127.0.0.1 .1.3.6.1.2.1.1.1.6=127.0.0.1
    23:50:51.229842 IP 127.0.0.1.35370 > 127.0.0.1.snmptrap:  Trap(215)  .1.3.6.1.2.1.1.1 127.0.0.1 enterpriseSpecific
    s=0 2296238946 .1.3.6.1.2.1.1.1.1="[username: test]"
    .1.3.6.1.2.1.1.1.2="TGT-1-wvfjuUoqmQj6rYmNbgOqQsXJkEt0E7BtFEzYFjMkDUFkfCYTWL-cas01.example.org" .1.3.6.1.2.1.1.1.3=2
    .1.3.6.1.2.1.1.1.4=1 .1.3.6.1.2.1.1.1.5=127.0.0.1 .1.3.6.1.2.1.1.1.6=127.0.0.1

