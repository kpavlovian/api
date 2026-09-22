@echo off
title DataValidatorApp - run

echo Checking for Java...
where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo [ERROR] Java not found in PATH.
    echo Install JDK 17 or newer: https://adoptium.net/
    echo Then run this file again.
    echo.
    pause
    exit /b 1
)

java -version
echo.

if not exist DataValidatorApp.jar (
    echo [ERROR] DataValidatorApp.jar not found in this folder.
    echo Run build.bat first to build the application.
    echo.
    pause
    exit /b 1
)

echo Starting application...
echo.
java -Dfile.encoding=UTF-8 -jar DataValidatorApp.jar

echo.
echo Application exited with code %errorlevel%.
pause
