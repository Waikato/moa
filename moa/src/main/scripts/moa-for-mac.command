#!/bin/bash
# ----------------------------------------------------------------------------
#  Copyright 2001-2006 The Apache Software Foundation.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# ----------------------------------------------------------------------------

#   Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
#   reserved.

#   Copyright (C) 2011-2019 University of Waikato, Hamilton, NZ

BASEDIR=`dirname $0`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
REPO=$BASEDIR/lib
CLASSPATH=$REPO/*

JCMD=java
if [ -f "$JAVA_HOME/bin/java" ]
then
  JCMD="$JAVA_HOME/bin/java"
fi

# check options
MEMORY=512m
MAIN=moa.gui.GUI
ARGS=
OPTION=
WHITESPACE="[[:space:]]"
for ARG in "$@"
do
  if [ "$ARG" = "-h" ] || [ "$ARG" = "-help" ] || [ "$ARG" = "--help" ]
  then
  	echo "Start script for MOA: Massive Online Analysis"
  	echo ""
  	echo "-h/-help/--help"
  	echo "    prints this help"
  	echo "-memory <memory>"
  	echo "    for supplying maximum heap size, eg 512m or 1g (default: 512m)"
  	echo "-main <classname>"
  	echo "    the class to execute (default: moa.gui.GUI)"
  	echo ""
  	echo "Note: any other options are passed to the Java class as arguments"
  	echo ""
  	exit 0
  fi

  if [ "$ARG" = "-memory" ] || [ "$ARG" = "-main" ]
  then
  	OPTION=$ARG
  	continue
  fi

  if [ "$OPTION" = "-memory" ]
  then
    MEMORY=$ARG
    OPTION=""
    continue
  elif [ "$OPTION" = "-main" ]
  then
    MAIN=$ARG
    OPTION=""
    continue
  fi

  if [[ $ARG =~ $WHITESPACE ]]
  then
    ARGS="$ARGS \"$ARG\""
  else
    ARGS="$ARGS $ARG"
  fi
done

# launch class
"$JCMD" \
  -classpath "$CLASSPATH" \
  -Xmx$MEMORY \
  -javaagent:"$REPO"/sizeofag-1.0.4.jar \
  $MAIN \
  $ARGS
