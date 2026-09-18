#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$JAR" ]; then
  exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  echo "gradle-wrapper.jar is not present; using system Gradle instead." >&2
  exec gradle "$@"
fi
echo "Gradle wrapper JAR is not present and system Gradle was not found." >&2
echo "Use the included GitHub Actions workflow to build the APK without Android Studio." >&2
exit 1
