@echo off
chcp 65001 >nul 2>&1
echo ============================================
echo  Akazukin - Test
echo ============================================
echo.
cd /d "%~dp0"

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java がインストールされていないか、PATH に設定されていません。
    echo   Java 17+ をインストールしてください: https://adoptium.net/
    pause
    exit /b 1
)

echo [1/1] テスト実行中...
call gradlew.bat test
if errorlevel 1 (
    echo.
    echo [WARN] 一部のテストが失敗しました。
    echo   レポート: 各モジュールの build/reports/tests/test/index.html
) else (
    echo.
    echo [OK] 全テスト成功!
)

echo.
echo ============================================
echo  テスト完了
echo ============================================
echo.
pause
