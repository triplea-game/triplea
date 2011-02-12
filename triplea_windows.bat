@echo off
java -Xmx512m -classpath bin/patch.jar;bin/triplea.jar  games.strategy.engine.framework.GameRunner
pause
