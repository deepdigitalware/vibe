@if "%DEBUG%" == "" @echo off
@rem -------------------------------------------------------------------------
@rem Gradle startup script for Windows
@rem -------------------------------------------------------------------------

setlocal

set DIR=%~dp0
set APP_HOME=%DIR%
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not "%JAVA_HOME%" == "" (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -version >NUL 2>&1
if %ERRORLEVEL% neq 0 (
  echo.
  echo ERROR: Java runtime not found. Please set JAVA_HOME or add java.exe to PATH.
  exit /b 1
)

set APP_ARGS=
set WRAPPER_MAIN=org.gradle.wrapper.GradleWrapperMain
set GRADLE_OPTS=-Dorg.gradle.appname=gradlew

"%JAVA_EXE%" %GRADLE_OPTS% -cp "%CLASSPATH%" %WRAPPER_MAIN% %*
endlocal