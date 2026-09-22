@echo off
REM Build without Maven (plain javac) - use this if Maven is not installed.
title DataValidatorApp - build

echo Checking for JDK (javac)...
where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo [ERROR] javac not found in PATH.
    echo You probably have only JRE installed, but JDK 17+ is required.
    echo Download: https://adoptium.net/temurin/releases/
    echo.
    pause
    exit /b 1
)

if not exist out mkdir out

echo Compiling...
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed - see messages above.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

echo Creating manifest...
if not exist out\META-INF mkdir out\META-INF
(
  echo Manifest-Version: 1.0
  echo Main-Class: ru.ket.validator.MainFrame
  echo.
) > out\META-INF\MANIFEST.MF

echo Building jar...
jar cfm DataValidatorApp.jar out\META-INF\MANIFEST.MF -C out .
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to create jar file.
    pause
    exit /b 1
)

echo.
echo Done: DataValidatorApp.jar
pause
