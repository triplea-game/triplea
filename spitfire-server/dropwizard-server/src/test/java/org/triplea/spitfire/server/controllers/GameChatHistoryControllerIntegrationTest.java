package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

@SuppressWarnings("UnmatchedTest")
class GameChatHistoryControllerIntegrationTest extends ControllerIntegrationTest {
  private final ModeratorChatClient client;

  GameChatHistoryControllerIntegrationTest(final URI localhost) {
    client = ModeratorChatClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void fetchGameChatHistory() {
    client.fetchChatHistoryForGame("game-id");
  }
}
