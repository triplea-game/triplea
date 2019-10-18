package org.triplea.server.lobby.game.hosting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.hosting.GameHostingClient;
import org.triplea.server.http.BasicEndpointTest;

class GameHostingControllerTest extends BasicEndpointTest<GameHostingClient> {

  GameHostingControllerTest() {
    super(GameHostingClient::newClient);
  }

  @Test
  void sendGameHostingRequest() {
    verifyEndpointReturningObject(GameHostingClient::sendGameHostingRequest);
  }
}
