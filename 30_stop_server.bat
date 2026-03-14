@echo off
echo Stopping Akazukin Dev Server...
echo.

REM Find and kill Quarkus dev mode process (Java process on port 8081)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":38081" ^| findstr "LISTENING"') do (
    echo Killing process PID: %%a
    taskkill /F /PID %%a 2>nul
)

REM Also kill any Gradle daemon that may be running quarkusDev
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr "PID:"') do (
    wmic process where "ProcessId=%%a" get CommandLine 2>nul | findstr "quarkusDev" >nul && (
        echo Killing Gradle quarkusDev process PID: %%a
        taskkill /F /PID %%a 2>nul
    )
)

echo.
echo Done. Server stopped.
pause
