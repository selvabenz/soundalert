#!/bin/sh
# SoundAlert uses Gradle 9.6.0. GitHub Actions provisions Gradle directly.
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Gradle is not installed and gradle-wrapper.jar is not present."
  echo "Use the included GitHub Actions workflow or run: gradle wrapper --gradle-version 9.6.0"
  exit 1
fi
exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
