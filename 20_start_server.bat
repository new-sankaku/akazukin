@echo off
title Р‘ВЄВЂВс - Dev Server
echo ==============================
echo  Р‘ВЄВЂВс - Dev Server Starting
echo  http://localhost:38081
echo ==============================
echo.
echo Р‘ВЄВЂВс
echo.
echo Press Ctrl+C to stop the server.
echo.
cd /d "%~dp0"

REM Launch Chrome after a short delay to allow server startup
start "" cmd /c "timeout /t 10 /nobreak >/dev/null && start chrome http://localhost:38081/login"

call gradlew.bat :akazukin-web:quarkusDev
echo.
echo Р‘ВЄВЂВс
