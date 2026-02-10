@echo off
echo Building Vibe Web App for Capacitor...

echo.
echo 1. Creating build directory...
if exist "dist" rmdir /s /q "dist"
mkdir "dist"

echo.
echo 2. Copying web assets...
xcopy "www" "dist" /E /I /H /Y

echo.
echo 3. Syncing with Capacitor...
npx cap sync

echo.
echo Web app built successfully!
echo.
echo To build the Android APK, you'll need to:
echo 1. Install Java JDK
echo 2. Set JAVA_HOME environment variable
echo 3. Run: npx cap build android
echo.
echo Alternatively, you can open the project in Android Studio:
echo npx cap open android
pause