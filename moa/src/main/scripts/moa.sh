#!/bin/bash

MEMORY=512m
java -Xmx$MEMORY -cp "../lib/moa-${project.version}:../lib/*" -javaagent:../lib/sizeofag-1.0.2.jar moa.gui.GUI

