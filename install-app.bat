@echo off
echo Installing Vibe App on connected Android device...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
if %errorlevel% == 0 (
    echo App installed successfully!
    echo Starting the app...
    adb shell am start -n com.vibe/.ui.SplashActivity
    echo App started successfully!
) else (
    echo Failed to install app. Make sure:
    echo 1. Your Android device is connected via USB
    echo 2. USB debugging is enabled on your device
    echo 3. You have accepted the debugging authorization
)
pause