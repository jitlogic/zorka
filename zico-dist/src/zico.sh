#!/bin/bash



Z_CMD="$1"

if [ -z "$Z_CMD" ] ; then
    echo "Usage: $0 {start|stop|run|status}"
    exit 1
fi

ZICO_HOME="$(dirname $0)"

if [ -z "$ZICO_HOME" ] ; then
    ZICO_HOME=$PWD
fi

if [[ "$ZICO_HOME" == '.' || $ZICO_HOME =~ ^\.\.?/.* ]] ; then
  ZICO_HOME="$PWD/$ZICO_HOME"
fi

for F in zico.conf zico.properties zico.war ; do
  if [ ! -f $ZICO_HOME/$F ] ; then
    echo "Incomplete collector installation: missing $F file in $ZICO_HOME."
  fi
done

for D in tmp log data db ; do
  if [ ! -d $ZICO_HOME/$D ] ; then
    echo "Missing directory: $ZICO_HOME/$D. Created."
    mkdir $ZICO_HOME/$D
  fi
done

. $ZICO_HOME/zico.conf

if [ -z "$JAVA_HOME" ] ; then
  echo "Missing JAVA_HOME setting. Add JAVA_HOME=/path/to/jdk7 to $ZICO_HOME/zico.conf."
  exit 1
fi

if [ -z "$Z_NAME" ] ; then
  Z_NAME="zico"
fi

#echo "ZICO_HOME: $ZICO_HOME"

status() {
    pgrep -f "Dzorka.app=$Z_NAME" >/dev/null
}

start() {
    if status ; then
      echo "Collector is running."
    else
      echo -n "Starting collector ..."
      cd $ZICO_HOME
      setsid $JAVA_HOME/bin/java -Dzorka.app=$Z_NAME $JAVA_OPTS -jar zico.war >$ZICO_HOME/log/console.log 2>&1 & 
      echo "OK."
    fi
}

run() {
    if status ; then
      echo "Another collector instance is running."
    else
      echo "Starting collector at $ZICO_HOME"
      cd $ZICO_HOME
      $JAVA_HOME/bin/java -Dzorka.app=$Z_NAME $JAVA_OPTS -jar zico.war
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

