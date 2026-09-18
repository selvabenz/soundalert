@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if exist "%JAR%" (
  java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo gradle-wrapper.jar is not present; using system Gradle instead.
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Gradle wrapper JAR is not present and system Gradle was not found.
echo Use the included GitHub Actions workflow to build the APK without Android Studio.
exit /b 1
