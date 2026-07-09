@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%tools\quick-view-latest-build.ps1"

if errorlevel 1 (
    echo.
    echo Quick view could not start. Check the message above for details.
    pause
)
