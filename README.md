
Zorka Agent & Zorka Spy
=======================

Zorka is a general purpose monitoring agent for Java application servers.

Basic features (among others):
* integration with commonly used monitoring systems: Zabbix, Nagios (planned);
* programmability - zorka can be extended using BeanShell scripts;
* bytecode instrumentation - zorka can instrument your code in several ways and present collected values via JMX Beans;
* (planned) mapped mbeans - user can map calcluated values from some mbeans into other mbeans (on attribute basis); standard JMX clients can fetch these values;
* (planned) rank lists - customizable thread ranks, mbean ranks etc.


Note that this is development snapshot of Zorka agent. While it works fairly well on author's production workloads, it still lacks documentation, its configuration directives and BSH APIs are still changing and getting it to work might be a challenge. In other works - it is still in flux. Documentation will appear as soon as things settle up.

