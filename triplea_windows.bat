@echo off
java -Xmx128m -classpath lib/patch.jar;classes;bin/triplea.jar;lib/looks-1.3.1.jar;  games.strategy.engine.framework.GameRunner
pause
