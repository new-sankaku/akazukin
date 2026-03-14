@echo off
chcp 65001 >nul 2>&1
echo ============================================
echo  Akazukin - Full Build
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

echo [1/1] ビルド実行中 (テストはスキップ)...
call gradlew.bat build -x test
if errorlevel 1 (
    echo.
    echo [ERROR] ビルドに失敗しました。上記のエラーを確認してください。
    pause
    exit /b 1
)

echo.
echo ============================================
echo  ビルド成功!
echo ============================================
echo.
echo  開発サーバー起動: 20_start_server.bat
echo  テスト実行:       gradlew.bat test
echo.
pause
