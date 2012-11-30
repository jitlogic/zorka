
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


