
Zorka Agent & Zorka Spy
=======================

Zorka is a powerful general purpose monitoring agent for your Java application servers. It is designed to
integrate seamlessly your Java applications with popular monitoring systems.

Basic features (among others):

* integration with commonly used monitoring systems and protocols: Zabbix, Nagios, syslog, SNMP;

* programmability - zorka can be extended using Beanshell scripts;

* bytecode instrumentation - zorka features very configurable and powerful instrumentation engine that can be used to
do virtually anything with collected values: aggregate statistics and present them in MBeans in various ways, send SNMP
traps, syslog messages, zabbix traps, intercept object references and present them directly via JMX or use them in any
way with Beanshell; instrumentation can be used for other tasks than basic monitoring (eg. audit trail);

* mapped mbeans - user can map calcluated values from anywhere into arbitrary mbean attributes; for example, you can
fetch some collection (list) anywhere from your application and present its size() function as some mbean attribute;
standard JMX clients can fetch these values (monitor presented collection length in our example);

* rank lists - customizable thread ranks, mbean ranks and ranks of methods instrumented by zorka;


Note that this is development snapshot of Zorka agent. While it works fairly well on author's production workloads,
its configuration directives and BSH APIs are still changing and getting it to work might sometimes be a challenge
(yet it works stable after being properly configured). In other works - it is still in flux: check download page often
for new versions.


Interesting links:

* [Introduction to Zorka](https://github.com/jitlogic/zorka/wiki/Intro)

* [Installation](https://github.com/jitlogic/zorka/wiki/Installation)

* [Configuration](https://github.com/jitlogic/zorka/wiki/Configuring-Zorka)

* [Zorka API Reference](https://github.com/jitlogic/zorka/wiki/Zorka-API-reference)

* [Examples](https://github.com/jitlogic/zorka/wiki/Examples)

* [Changelog](https://github.com/jitlogic/zorka/wiki/CHANGES)

* [Downloads](http://code.google.com/p/zorka/downloads/list)

