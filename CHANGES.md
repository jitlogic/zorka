Zorka 1.0.13 (2015-05-20)
-------------------------

* LDAP login for ZICO collector


Zorka 1.0.12 (2015-03-01)
-------------------------

* get rid of gson dependency;
* various fixes;


Zorka 1.0.11 (2015-02-01)
-------------------------

* automatically determine stack frame computation (JDK7+);
* agent: fixes (MySQL, thread pools);
* collector: fixes (NPE when resetting password);


Zorka 1.0.10 (2015-01-01)
-------------------------

* zorka: performance SLA calculation for ZorkaStats;
* zico: reduce memory consumption;
* several bufixes;


Zorka 1.0.9 (2014-10-13)
------------------------

* fix: proper ASM5 instruction set version in ASM class reader;
* ZICO: pivot (aggregate) view of collected traces;
* zico-util: improve parallelism of index rebuild op (by sorting host dirs by size);

Zorka 1.0.8 (2014-09-29)
------------------------

* active checks: fixes, new templates;
* ZICO UI: many fixes, regexp search;


Zorka 1.0.7 (2014-09-05)
------------------------

* more fixes to ZICO collector;
* HTTP/SQL/EJB tagging and classification;
* active checks for Zabbix (makotoiguchi);


Zorka 1.0.6 (2014-07-27)
------------------------

* bugfix release


Zorka 1.0.5 (2014-07-22)
------------------------

* CAS integration;
* fixes and refactorings;


Zorka 1.0.4 (2014-05-27)
------------------------

* lots of UI bugs fixed after scrapping GXT;
* use REST+JSON for communication between UI and collector (get rid of RequestFactory);
* ZICO collector now requires Java 8 to work;


Zorka 1.0.3 (2014-05-19)
------------------------

* enhance nagios commands framework, predefined commands for JVM, HTTP, SQL etc.
* update dependencies: asm, fressian;
* revamp ZICO UI (get rid of GXT, upgrade to GWT 2.6, Jetty 9);


Zorka 1.0.2 (2014-05-03)
------------------------

* hide BSH scripts inside agent jar (yet scripts/ directory for user scripts still works)
* JMX values scanner for Nagios - with aggregates and other features;


Zorka 1.0.1 (2014-03-31)
------------------------

* grouping hosts in ZICO collector;
* bugfixes;


Zorka 1.0.0 (2014-03-01)
-------------------------

* support for JDK 8 (generating stack maps for instrumented code);
* Tomcat 8 now officially supported;
* UI: filter traces by timestamp, other EQL fixes;
* UI: fixes for safe mode and host properties editing;


Zorka 0.9.19 (2014-02-22)
-------------------------

 * Glassfish 4.0 support;
 * agent: util.tapInputStream(), util.tapOutputStream() for wiretaping streams;
 * SOAP monitoring: support for Apache CXF framework;
 * REST monitoring: general config + support for Apache CXF framework;


Zorka 0.9.18 (2014-02-15)
-------------------------

 * SOAP monitoring (with Axis 1.x support, more to come);
 * Wildfly 8.0 + Undertow support;
 * JBoss Management (DMR) access via zorka agent;


Zorka 0.9.17 (2014-02-08)
-------------------------

 * support for Spring MVC tracing/monitoring;
 * support for Quartz tracing/monitoring
 * fix: data corruption in ZICO collector;


Zorka 0.9.16 (2014-02-01)
-------------------------

 * fixes for agent operating in Windows env (paths, log buffering etc.);
 * various collector / UI fixes;
 * JMS monitoring support;
 * file/zabbix/syslog trappers for EJB/HTTP/SQL/JMS slow/error query logs;
 * rudimentary application auditing framework (only JBoss7 supported at the moment);


Zorka 0.9.15 (2014-01-15)
-------------------------

 * lot of fixes, collector and collector UI;


Zorka 0.9.14 (2014-01-02)
-------------------------

 * collector: rewrite trace indexing and search engine; use MapDB+RDS instead of SQL;
 * lots of fixes;


Zorka 0.9.13 (2013-12-16)
-------------------------

 * agent: online reconfiguration (via `zorka.reload[]` or built in MBean);
 * agent: jetty 6.x support script;
 * collector: user database with access control to particular hosts for ordinary users;
 * collector: faster method call tree;
 * lots of small fixes as usual;


Zorka 0.9.12 (2013-11-20)
-------------------------

 * agent: websphere support (HTTP, EJB3 tracing, PMI access, rudimentary PMI templates for zabbix);
 * agent: MuleESB support (tracing flows, dispatchers,flow components, maintaining ZorkaStats, zabbix templates etc.);
 * agent: LDAP monitoring (tracing LDAP searches, maintaining ZorkaStats);
 * agent: improve HTTP tracing (headers, cookies, redirections);
 * agent: revive and extend zabbix templates for JBoss, Tomcat etc.
 * agent: improve ZICO performance with bulk ZICO transfers;
 * agent: prioritized class/method matchers;
 * agent: `zorka.require()` function to include scripts from other scripts;
 * agent: get rid of profiles, rework scripts to be included directly from `zorka.properties`;
 * agent: tracer tuning embedded directly in agent scripts (no need for manual tuning for every new application);
 * agent: instrumentation engine statistics: time spent on matching, instrumentation etc. (available as ZorkaStats);
 * agent: throughput monitoring implemented in ZorkaStats;
 * collector: asynchronous DB writer for TRACES table to eliminate lags caused by MySQL hiccups;
 * collector: admin and viewer roles for collector;
 * collector: option to disable/enable collection from particular hosts available from UI;
 * collector: case-insensitive search in trace detail panel (full-text only for now);
 * lots of small fixes;


Zorka 0.9.11 (2013-10-21)
-------------------------

 * agent: checksum calculating spy processors;
 * agent: trace attribute propagation functions;
 * collector: new query language for searching methods inside trace;
 * collector: lots of UI fixes and polishes;
 * collector: enhance self-monitoring (trace and monitor submission chain);


Zorka 0.9.10 (2013-09-30)
-------------------------

 * rename zorka-central -> zico;
 * match classes and methods by interface (with recursion);


Zorka 0.9.9 (2013-09-15)
------------------------

 * many improvements and fixes in zorka-central;
 * unified scripts and  for JDBC monitoring;


Zorka 0.9.8 (2013-09-01)
------------------------

 * zorka-central - (rudimentary) network trace collector;
 * agent profiles - multiple script configuration in single directory;
 * usual fixes;


Zorka 0.9.7 (2013-07-07)
------------------------

 * use fressian format for saving traces;
 * congestion monitoring (via ZorkaStats);
 * lots of fixes;


Zorka 0.9.6 (2013-05-01)
------------------------

 * better security with zorka.allow() function;
 * load additional property files with zorka.loadCfg() function;
 * programmatic tracer attribute setting with tracer.newAttr() function;
 * lots ot fixes


Zorka 0.9.5 (2013-04-15)
------------------------

 * hiccup meter (based on Azul's hiccup metering utility);
 * tracer support for WSO2;
 * JBoss7 domain mode support (zorka home dir now can be passed as system property);
 * microsecond and nanosecond resolution for MethodCallStats;
 * proper query execution timeout handling in BSH agent;


Zorka 0.9.4 (2013-04-02)
------------------------

 * unify argument ordering of Spy;
 * sample JDBC script for PostgreSQL (long queries, trace attrs, stats);
 * refactor zorka-common+zorka-agent -> zorka-core+zorka-agent;
 * bugfixes;


Zorka 0.9.3 (2013-03-23)
------------------------

 * support for JDK5;
 * fixes;


Zorka 0.9.2 (2013-03-11)
------------------------

 * enhance trace viewer, get rid of unnecessary dependencies;
 * new 'local max' attribute maintained by method call statistics;


Zorka 0.9.1 (2013-02-28)
------------------------

 * zorka diagnostics (with zabbix template) - mbean for monitoring agent health
 * lots of fixes for viewer and agent itself;


Zorka 0.9 (2013-02-05)
----------------------

 * tracer enhancements and bugfixes;
 * performance metrics collector that adds collected data to tracer files;
 * new JMX query / object graph traversal DSL;
 * `zabbix.discovery()` now uses JMX query DSL;
 * convenient API for accessing `zorka.properties` settings from BSH scripts;


Zorka 0.8 (2013-01-20)
---------------------

 * rudimentary method call tracer implemented;
 * zorka-viewer: trace files viewer;


Zorka 0.7 (2013-01-06)
----------------------

 * refactor spy API to use string keys (instead of convoluted stage-slot convention);
 * get rid od ON_COLLECT stage, use asynchronous queue collector;
 * refactor logger to act as aggregator of (attachable) trappers, remove loggger <-> file trapper redundancies;
 * lots of redundant code eliminated (eg. threading code in trappers, rank listers, agent integrations etc.)
 * lots of cleanups, javadocs, bugfixes, simplify package structure and limit inter-package dependencies;
 * performance optimization of ZorkaStatsCollector (and removal of JmxAttrCollector);
 * rudimentary stress testing / microbenchmarking framework implemented;
 * spy now supports OSGi-based frameworks (eg. WSO2 Carbon, see sample script on how to configure it);
 * remove custom pool executors, use standard ThreadPoolExecutor instead;
 * support for matching classes and methods by annotations;


Zorka 0.6 (2012-12-22)
----------------------

 * normalization of xQL queries (all major query languages);
 * normalization of LDAP search queries;
 * file trapper (logs events to files instead of syslog/zabbix/SNMP);
 * composite processing chains and comparator filters;
 * zorka API overhaul (yet more refactoring are on the way);


Zorka 0.5 (2012-12-07)
----------------------

 * syslog trapper support;
 * snmp trapper support (traps and value get interface);
 * zabbix trapper;
 * config scripts and zabbix templates for JBoss 7;
 * documentation: converted to `md` format; more interesting examples (eg. CAS auditing);


Zorka 0.4 (2012-11-26)
----------------------

 * documentation updates, cleanups and fixes (as usual);
 * nagios NRPE protocol support;
 * thread rank ported to new ranking framework;
 * new - circular buffer aggregate;


Zorka 0.3 (2012-11-10)
----------------------

 * basic ranking framework (working with Zorka MethodCallStats);
 * implement missing collectors of ZorkaSpy;
 * implement missing processors in ZorkaSpy;
 * support for IBM JDK and JRockit;
 * documentation updates, cleanups and fixes;


Zorka 0.2 (2012-11-04)
---------

 * new ZorkaSpy instrumentation engine (incomplete, yet functional);
 * get rid of lib/*.jar files, embed them into agent.jar;
 * get rid of j2ee dependencies (use reflection instead);
 * remove zorka5.sar module and jboss dependencies (use instrumentation instead);
 * documentation updates, many little cleanups and fixes;


Zorka 0.1 (2012-09-19)
----------------------

 * initial release;
 * zabbix integration;
 * basic functions for accessing JMX data;
 * rudimentary instrumentation engine (ZorkaSpy);


