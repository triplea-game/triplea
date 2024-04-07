package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.ModeratorLobbyClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class GameChatHistoryControllerIntegrationTest extends ControllerIntegrationTest {
  private final ModeratorLobbyClient client;

  GameChatHistoryControllerIntegrationTest(final URI localhost) {
    client = ModeratorLobbyClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void fetchGameChatHistory() {
    client.fetchChatHistoryForGame("game-id");
  }
}
