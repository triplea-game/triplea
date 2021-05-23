package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingClient;
import org.triplea.spitfire.server.BasicEndpointTest;

@SuppressWarnings("UnmatchedTest")
class GameHostingControllerTest extends BasicEndpointTest<GameHostingClient> {

  GameHostingControllerTest(final URI localhost) {
    super(localhost, GameHostingClient::newClient);
  }

  @Test
  void sendGameHostingRequest() {
    verifyEndpointReturningObject(GameHostingClient::sendGameHostingRequest);
  }
}
