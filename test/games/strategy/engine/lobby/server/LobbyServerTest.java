package games.strategy.engine.lobby.server;

import org.junit.Before;
import org.junit.Test;

public class LobbyServerTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void lobbyServerStartsAndStops() {
    LobbyServer.main(new String[0]);
    LobbyServer.stopServer();
  }

}
