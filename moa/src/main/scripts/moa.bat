@echo off

set BASEDIR=%~dp0\..
set MEMORY=512m
IF "%PROCESSOR_ARCHITECTURE%"=="x86" (set ARCH=32BIT) else (set ARCH=64BIT)

if "%ARCH%" == "32BIT" set ARCHLIB=%BASEDIR%/windows32/*
if "%ARCH%" == "64BIT" set ARCHLIB=%BASEDIR%/windows64/*

java -Xmx%MEMORY% -cp "%BASEDIR%/lib/moa-${project.version};%BASEDIR%/lib/*;%ARCHLIB%" -javaagent:%BASEDIR%/lib/sizeofag-1.0.2.jar moa.gui.GUI

