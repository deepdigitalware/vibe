#!/usr/bin/env sh
# -------------------------------------------------------------------------
# Gradle startup script for Unix
# -------------------------------------------------------------------------

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Try JAVA_HOME, then fall back to java from PATH
if [ -n "$JAVA_HOME" ] ; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE=java
fi

"$JAVA_EXE" -version >/dev/null 2>&1 || {
  echo "ERROR: Java runtime not found. Set JAVA_HOME or add java to PATH." >&2
  exit 1
}

WRAPPER_MAIN=org.gradle.wrapper.GradleWrapperMain
GRADLE_OPTS="-Dorg.gradle.appname=gradlew"

exec "$JAVA_EXE" $GRADLE_OPTS -cp "$CLASSPATH" $WRAPPER_MAIN "$@"