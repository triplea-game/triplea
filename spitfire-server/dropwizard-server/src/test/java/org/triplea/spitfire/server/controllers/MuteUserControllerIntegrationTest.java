package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorLobbyClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class MuteUserControllerIntegrationTest extends ControllerIntegrationTest {
  private final ModeratorLobbyClient client;

  MuteUserControllerIntegrationTest(final URI localhost) {
    this.client = ModeratorLobbyClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void muteUser() {
    client.muteUser(PlayerChatId.of("chat-id"), 600);
  }
}
