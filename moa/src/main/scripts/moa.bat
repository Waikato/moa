@echo off

set BASEDIR=%~dp0\..
set MEMORY=512m
java -Xmx%MEMORY% -cp "%BASEDIR%/lib/moa-${project.version}../lib/*" -javaagent:%BASEDIR%/lib/sizeofag-1.0.2.jar moa.gui.GUI

