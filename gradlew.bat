@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo gradle-wrapper.jar is not included in this chat-generated archive.
  echo Run: gradle wrapper --gradle-version 9.6.0
  echo or open the project in Android Studio and regenerate the Gradle wrapper.
  exit /b 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
