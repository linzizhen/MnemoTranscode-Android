@echo off
set JAVA_HOME=D:\code\MTC-A\jdk17\jdk-17.0.9
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME: %JAVA_HOME%
java -version
cd /d D:\code\MTC-A\android
call gradlew.bat assembleDebug
