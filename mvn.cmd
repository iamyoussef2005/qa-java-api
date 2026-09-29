@REM ----------------------------------------------------------------------------
@REM Maven Runner Script
@REM ----------------------------------------------------------------------------
@echo off
set "MAVEN_EXE=C:\Users\ASUS TUF\.m2\wrapper\dists\apache-maven-3.9.14-bin\1cb7fhup6b5n3bed6kckbrnspv\apache-maven-3.9.14\bin\mvn.cmd"
if exist "%MAVEN_EXE%" (
    "%MAVEN_EXE%" %*
) else (
    mvn %*
)
