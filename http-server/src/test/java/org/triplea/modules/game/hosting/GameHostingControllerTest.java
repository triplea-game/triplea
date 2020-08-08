package org.triplea.modules.game.hosting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingClient;
import org.triplea.modules.http.BasicEndpointTest;

class GameHostingControllerTest extends BasicEndpointTest<GameHostingClient> {

  GameHostingControllerTest() {
    super(GameHostingClient::newClient);
  }

  @Test
  void sendGameHostingRequest() {
    verifyEndpointReturningObject(GameHostingClient::sendGameHostingRequest);
  }
}
