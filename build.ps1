$env:JAVA_HOME = "D:\code\MTC-A\jdk17\jdk-17.0.9"
$env:PATH = "D:\code\MTC-A\jdk17\jdk-17.0.9\bin" + [System.IO.Path]::PathSeparator + $env:PATH
Set-Location "D:\code\MTC-A\android"
& .\gradlew.bat assembleDebug
