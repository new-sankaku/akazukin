@echo off
if not defined WEZTERM_EXECUTABLE (
    start "" "C:\Program Files\WezTerm\wezterm-gui.exe" start -- cmd /c "%~f0" %*
    exit /b
)
cd /d "%~dp0"
claude --dangerously-skip-permissions --chrome