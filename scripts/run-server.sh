cd $(dirname $0)
java -server  -Xmx192m -classpath bin/triplea.jar:lib/derby-10.10.1.1.jar -Dtriplea.lobby.port=3303 -Dtriplea.lobby.console=true  games.strategy.engine.lobby.server.LobbyServer 
