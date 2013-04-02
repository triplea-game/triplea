@echo off
java -Xmx1024m -classpath bin/patch.jar;bin/triplea.jar  games.strategy.engine.framework.GameRunner
pause
