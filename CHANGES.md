Zorka 0.6 (2012-12-21)
----------------------

 * normalization of xQL queries (all major languages);
 * normalization of LDAP search queries;
 * file trapper (logs events to files instead of syslog/zabbix/SNMP);
 * composite processing chains and comparator filters;


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
