@echo off
REM Builds and launches the current Small Front source tree.
REM Gradle is incremental, so unchanged launches remain fast while stale JARs are avoided.
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"

pushd "%~dp0"
echo Building current Small Front source...
call gradlew.bat :game-headed:shadowJar
if errorlevel 1 (
  echo Build failed. Small Front was not launched.
  popd
  exit /b 1
)

set "JAR="
for /f "delims=" %%J in ('dir /b /o-d "game-app\game-headed\build\libs\game-headed-*.jar" 2^>nul') do (
  set "JAR=%~dp0game-app\game-headed\build\libs\%%J"
  goto :found
)
echo No JAR was produced by the build.
popd
exit /b 1

:found
for /f "delims=" %%G in ('git rev-parse --short HEAD 2^>nul') do set "COMMIT=%%G"
echo Launching %JAR%
if defined COMMIT echo Source commit: %COMMIT%
popd
"%JAVA_HOME%\bin\java" -jar "%JAR%" %*
