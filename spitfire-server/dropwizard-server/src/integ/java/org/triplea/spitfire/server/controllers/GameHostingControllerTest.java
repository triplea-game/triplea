package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class GameHostingControllerTest extends ControllerIntegrationTest {
  private final GameHostingClient client;

  GameHostingControllerTest(final URI localhost) {
    client = GameHostingClient.newClient(localhost);
  }

  @Test
  void sendGameHostingRequest() {
    final var result = client.sendGameHostingRequest();
    assertThat(result, is(IsNull.notNullValue()));
  }
}
