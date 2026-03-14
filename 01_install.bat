@echo off
chcp 65001 >nul 2>&1
echo ============================================
echo  Akazukin - Install / Build
echo ============================================
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH.
    echo   Please install Java 17+ from https://adoptium.net/
    echo   (Java 21 will be auto-downloaded by Gradle Toolchain)
    pause
    exit /b 1
)

echo [1/2] Downloading dependencies and building...
call gradlew.bat build -x :akazukin-infrastructure:quarkusAppPartsBuild -x :akazukin-web:quarkusAppPartsBuild -x :akazukin-infrastructure:quarkusBuild -x :akazukin-web:quarkusBuild
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed. Check the error messages above.
    pause
    exit /b 1
)

echo.
echo [2/2] Running tests...
call gradlew.bat test
if errorlevel 1 (
    echo.
    echo [WARN] Some tests failed. Check reports in each module's build/reports/tests/test/index.html
) else (
    echo [OK] All tests passed!
)

echo.
echo ============================================
echo  Build completed successfully!
echo ============================================
echo.
echo  To start the dev server:
echo    gradlew.bat :akazukin-web:quarkusDev
echo.
echo  Prerequisites for running:
echo    - PostgreSQL (or Docker for Dev Services)
echo    - Docker (optional, for LocalStack/SQS)
echo.
pause
