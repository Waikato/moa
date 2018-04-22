#!/bin/bash

BASEDIR=`dirname $0`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
MEMORY=512m
case "`uname`" in
  Darwin*) ARCHLIB="$BASEDIR/lib/macosx64/*"
    ;;
  *)
    if [ `uname -m` == "i686" ]
    then
      ARCHLIB="$BASEDIR/lib/linux32/*"
    else
      ARCHLIB="$BASEDIR/lib/linux64/*"
    fi
esac
java -Xmx$MEMORY -cp "$BASEDIR/lib/moa-${project.version}:$BASEDIR/lib/*:$ARCHLIB" -javaagent:$BASEDIR/lib/sizeofag-1.0.2.jar moa.gui.GUI

