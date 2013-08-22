#!/bin/sh

# Uncomment and adjust if there is no system wide JAVA_HOME set
#JAVA_HOME=/opt/jdk7


if [ -f central.war -a -f central.properties ] ; then
    CENTRAL_HOME=$(pwd)
else
    CENTRAL_HOME=$(dirname $0)
fi

cd $CENTRAL_HOME

JAVA_OPTS="-Xmx1024m -XX:MaxPermSize=256m -Dcentral.home.dir=$(pwd)"

PATH=$JAVA_HOME/bin:$PATH

setsid java $JAVA_OPTS -jar central.war >> log/stdout.log &

