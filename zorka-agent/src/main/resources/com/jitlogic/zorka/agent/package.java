/*
 * TODO basic JMX function
 * TODO basic getters for various objects (partly implemented in zoola, partly in zorka-agent)
 * TODO basic java system information (via ps etc.)
 * TODO zabbix integration (ZABBIX protocol), map zoola functions to zabbix keys
 * TODO nagios integration (NRPE protocol), map zoola functions to nagios checks  
 * TODO windowed rate calculators - deklarowane z nazwy w skrypcie konfiguracyjnym 
 * TODO pluggable ranking framework (for threads, beans etc. - configurable via zoola script)
 */

/**
 * Installing agent: -javaagent:/path/to/zorka.jar -Dzorka.home.dir=/opt/zorka/tomcat7  
 * -Dcom.sun.management.jmxremote
 * -Dcom.sun.management.jmxremote.authenticate=false
 * -Dcom.sun.management.jmxremote.ssl=false
 * -Dcom.sun.management.jmxremote.port=33001
 * -
 * 
 */
