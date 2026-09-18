@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo Gradle is not installed and gradle-wrapper.jar is not present.
  echo Use the included GitHub Actions workflow or run: gradle wrapper --gradle-version 9.6.0
  exit /b 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
