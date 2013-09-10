#!/bin/bash



Z_CMD="$1"

if [ -z "$Z_CMD" ] ; then
    echo "Usage: $0 {start|stop|run|status}"
    exit 1
fi

Z_HOME="$(dirname $0)"

if [ -z "$Z_HOME" ] ; then
    Z_HOME=$PWD
fi

if [[ "$Z_HOME" == '.' || $Z_HOME =~ ^\.\.?/.* ]] ; then
  Z_HOME="$PWD/$Z_HOME"
fi

for F in central.conf central.properties central.war ; do
  if [ ! -f $Z_HOME/$F ] ; then
    echo "Incomplete collector installation: missing $F file in $Z_HOME."
  fi
done

for D in tmp log data db ; do
  if [ ! -d $Z_HOME/$D ] ; then
    echo "Missing directory: $Z_HOME/$D. Created."
    mkdir $Z_HOME/$D
  fi
done

. $Z_HOME/central.conf

if [ -z "$JAVA_HOME" ] ; then
  echo "Missing JAVA_HOME setting. Add JAVA_HOME=/path/to/jdk7 to $Z_HOME/central.conf."
  exit 1
fi

if [ -z "$Z_NAME" ] ; then
  Z_NAME="central"
fi

#echo "Z_HOME: $Z_HOME"

status() {
    pgrep -f "Dzorka.app=$Z_NAME" >/dev/null
}

start() {
    if status ; then
      echo "Collector is running."
    else
      echo -n "Starting collector ..."
      cd $Z_HOME
      setsid $JAVA_HOME/bin/java -Dzorka.app=$Z_NAME $JAVA_OPTS -jar central.war >$Z_HOME/log/console.log 2>&1 & 
      echo "OK."
    fi
}

run() {
    if status ; then
      echo "Another collector instance is running."
    else
      echo "Starting collector at $Z_HOME"
      cd $Z_HOME
      $JAVA_HOME/bin/java -Dzorka.app=$Z_NAME $JAVA_OPTS -jar central.war
    fi
}

stop() {
    if status ; then
      echo -n "Stopping collector ..."
      pkill -f Dzorka.app=$Z_NAME >/dev/null
      echo "OK"
    else
      echo -n "Collector already stopped."
    fi
}

case "$Z_CMD" in
start)
    start
    ;;
stop)
    stop
    ;;
run)
    run
    ;;
status)
    if status ; then
      echo "Collector is running."
      exit 0
    else
      echo "Collector is not running."
      exit 1
    fi
    ;;
esac

