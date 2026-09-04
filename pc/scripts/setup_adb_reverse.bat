@echo off
echo ================================================================
echo  Master Companion — ADB USB Port Forwarding / Reverse Tether
echo ================================================================
echo.

where adb >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] ADB not found in system PATH.
    echo Please make sure Android platform-tools is installed.
    pause
    exit /b 1
)

echo [1/3] Checking connected Android devices...
adb devices
echo.

echo [2/3] Setting up reverse TCP port 8420 (Command Bridge)...
adb reverse tcp:8420 tcp:8420

echo [3/3] Setting up forward UDP port 8421 (Audio Streaming)...
adb forward tcp:8421 tcp:8421

echo.
echo ================================================================
echo  Ports successfully mapped!
echo  Command Bridge: http://127.0.0.1:8420
echo ================================================================
pause
