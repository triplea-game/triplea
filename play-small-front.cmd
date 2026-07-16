@echo off
REM Launches the Small Front build. Both maps need the custom delegates in this tree,
REM so a stock TripleA release will not load them.
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"

for /f "delims=" %%J in ('dir /b /o-d "%~dp0game-app\game-headed\build\libs\game-headed-*.jar" 2^>nul') do (
  set "JAR=%~dp0game-app\game-headed\build\libs\%%J"
  goto :found
)
echo No jar found. Build one first:
echo     gradlew :game-headed:shadowJar
exit /b 1

:found
echo Launching %JAR%
"%JAVA_HOME%\bin\java" -jar "%JAR%" %*
