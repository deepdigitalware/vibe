@echo off
echo Starting Vibe App Services...

echo.
echo 1. Starting Backend Server (Port 8888)...
start /min cmd /c "cd server && node index.js ^& pause"

echo.
echo 2. Starting Admin Panel (Port 8001)...
start /min cmd /c "cd server && node admin.js ^& pause"

echo.
echo 3. Starting APK Server (Port 8000)...
start /min cmd /c "node serve-apk.js ^& pause"

echo.
echo Services started successfully!
echo.
echo Backend Server: http://localhost:8888
echo Admin Panel: http://localhost:8001/
echo APK Server: http://192.168.1.5:8000
echo Management Console: http://192.168.1.5:8000/manage
echo.
echo To install the app on your Android device:
echo 1. Make sure your device is on the same Wi-Fi network
echo 2. Open browser on your Android device
echo 3. Go to: http://192.168.1.5:8000
echo 4. Download and install the APK
echo.
echo Opening Management Console in your browser...
start http://192.168.1.5:8000/manage
echo.
echo Press any key to exit...
pause >nul