@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Resolve repo root (this BAT is expected at tools\launch\)
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\") do set "REPO_ROOT=%%~fI"

REM Path to our custom RuneLite shaded jar
set "CLIENT_JAR=%REPO_ROOT%RuneLiteCustom\runelite-client\target\client-1.12.7-SNAPSHOT-shaded.jar"

if not exist "%CLIENT_JAR%" (
    echo [ERROR] Could not find custom RuneLite jar at:
    echo         %CLIENT_JAR%
    echo Build it first from RuneLiteCustom with:
    echo   mvn -U -DskipTests clean package
    pause
    exit /b 1
)

REM Locate java.exe
for %%J in (java.exe) do (
    for /f "delims=" %%P in ('where %%J 2^>nul') do (
        set "JAVA_BIN=%%P"
        goto :foundjava
    )
)

echo [ERROR] Could not locate java.exe.
pause
exit /b 1

:foundjava

echo Launching RuneMirror Host...
echo Using Java: %JAVA_BIN%
echo Jar: %CLIENT_JAR%
echo.

pushd "%REPO_ROOT%RuneLiteCustom\runelite-client\target"
"%JAVA_BIN%" -Duser.home="C:\Users\James\Desktop\RuneMirrorHostHome" -Xmx768m -Xss2m -XX:CompileThreshold=1500 -jar "%CLIENT_JAR%"
popd

endlocal
