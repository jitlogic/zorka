rem Zorka Intranet Collector startup script for Win32/Win64 platforms.
rem
rem This is trivial startup script not really suitable for production use.
rem Using some kind of service wrapper for windows is recommended and will
rem appear some future version. 
rem
rem Zorka Intranet Collector hasn't been tested well on Windows platforms,
rem in particular its limitations are not known. Use Windows only if you
rem must, otherwise consider Linux. Collector does not have huge requirements
rem and fits well into existing Zabbix server (if you have one).

rem Paths to collector and JDK7 need to be set correctly
set ZICO_HOME=C:\zico
set JAVA_HOME=C:\Java\jdk1.7.0_45

rem Mandatory java options (no need for change in most cases)
set JAVA_OPTS=-Xms512m -Xmx2048m -XX:MaxPermSize=256m -XX:-UseSplitVerifier
set JAVA_OPTS=%JAVA_OPTS% -Dlogback.configurationFile=%ZICO_HOME%/logback.xml
set JAVA_OPTS=%JAVA_OPTS% -Dzico.home.dir=%ZICO_HOME%
set JAVA_OPTS=%JAVA_OPTS% -javaagent:%ZICO_HOME%\zorka.jar=%ZICO_HOME%

cd %ZICO_HOME%
%JAVA_HOME%\bin\java %JAVA_OPTS% -jar zico.war 


