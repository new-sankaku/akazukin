@echo off
chcp 65001 >nul 2>&1
echo ============================================
echo  Akazukin - Build ^& Test
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

echo [1/2] ビルド実行中 (テンプレート検証含む)...
call gradlew.bat build -x test --console=plain -q 2> build_error.log
if errorlevel 1 (
    echo.
    echo [ERROR] ビルドに失敗しました:
    type build_error.log
    del build_error.log >nul 2>&1
    pause
    exit /b 1
)
del build_error.log >nul 2>&1
echo [OK] ビルド成功
echo.

echo [2/2] テスト実行中...
call gradlew.bat test --console=plain -q 2> test_error.log
if errorlevel 1 (
    echo.
    echo [ERROR] テスト失敗:
    type test_error.log
    del test_error.log >nul 2>&1
    pause
    exit /b 1
)
del test_error.log >nul 2>&1

echo.
echo ============================================
echo  ビルド ^& テスト 全て成功!
echo ============================================
echo.
pause
