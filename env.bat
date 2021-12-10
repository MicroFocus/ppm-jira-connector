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
set PPM_SERVER_ROOT=C:\ppm\SourceCode\java\target\jboss\server\_common
set JDK_LIB_DIR=C:\Java\jdk1.8.0_261\jre\lib


REM ####################################################
REM    You should have ANT_HOME and JAVA_HOME defined
REM ####################################################
REM
REM set JAVA_HOME=c:\java\jdk8
REM set ANT_HOME=C:\ant\apache-ant-1.6.2
REM set PATH=%JAVA_HOME%\bin;%ANT_HOME%\bin;%PATH%
REM

REM ####################################################
REM    ANT Related Environment.
REM ####################################################
set ANT_OPTS=-Xmx1024m -Dfile.encoding=UTF-8
set ANT_ARGS=-lib %PPM_SERVER_ROOT%\deploy\itg.war\WEB-INF\lib
set JDK_LIB=%JDK_LIB_DIR%\rt.jar;%JDK_LIB_DIR%\tools.jar
set SOURCE=1.8
set TARGET=1.8

echo   JAVA_HOME %JAVA_HOME%
echo   ANT_HOME %ANT_HOME%
echo   ANT_ARGS %ANT_ARGS%
echo   PPM_SERVER_ROOT %PPM_SERVER_ROOT%
echo   JDK_LIB   %JDK_LIB%
echo   SOURCE    %SOURCE%
echo   TARGET    %TARGET%