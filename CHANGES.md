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
