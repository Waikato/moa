@echo off

set BASEDIR=%~dp0\..
set MEMORY=512m
IF "%PROCESSOR_ARCHITECTURE%"=="x86" (set ARCH=32BIT) else (set ARCH=64BIT)

java -Xmx%MEMORY% -cp "%BASEDIR%/lib/*" -javaagent:"%BASEDIR%/lib/sizeofag-1.0.4.jar" moa.gui.GUI

