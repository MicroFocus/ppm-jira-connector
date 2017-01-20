@echo off

REM ####################################################
REM
REM          PPM Agile SDK Connector 
REM          Build & Bundle packaging
REM
REM ####################################################

REM ####################################################
REM    PPM Root
REM ####################################################
set PPM_SERVER_ROOT=C:\PPM_HOME\server\kintana
set JDK_LIB_DIR=C:\Java\jdk7\jre\lib


REM ####################################################
REM    You should have ANT_HOME and JAVA_HOME defined
REM ####################################################
REM
REM set JAVA_HOME=c:\java\jdk7
REM set ANT_HOME=C:\ant\apache-ant-1.6.2
REM set PATH=%JAVA_HOME%\bin;%ANT_HOME%\bin;%PATH%
REM

REM ####################################################
REM    ANT Related Environment.
REM ####################################################
set ANT_OPTS=-Xmx1024m -Dfile.encoding=UTF-8
set ANT_ARGS=-lib %PPM_SERVER_ROOT%\deploy\itg.war\WEB-INF\lib
set JDK_LIB=%JDK_LIB_DIR%\rt.jar;%JDK_LIB_DIR%\tools.jar
set SOURCE=1.7
set TARGET=1.7

echo   JAVA_HOME %JAVA_HOME%
echo   ANT_HOME %ANT_HOME%
echo   ANT_ARGS %ANT_ARGS%
echo   PPM_SERVER_ROOT %PPM_SERVER_ROOT%
echo   JDK_LIB   %JDK_LIB%
echo   SOURCE    %SOURCE%
echo   TARGET    %TARGET%