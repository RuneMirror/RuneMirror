@echo off
REM Build the Axiom RuneLite plugin
echo Building Axiom RuneLite plugin...
cd /d "%~dp0"
cd runelite-client
call mvn clean package -DskipTests
if errorlevel 1 (
    echo.
    echo Build failed! Check the error messages above.
    pause
    exit /b 1
)
echo.
echo Build successful! Plugin JAR is in: runelite-client\target\
echo.
echo To install the plugin:
echo 1. Copy the JAR from runelite-client\target\client-1.12.7-SNAPSHOT-shaded.jar
echo 2. Or use the RuneLite development mode (recommended)
echo.
pause

