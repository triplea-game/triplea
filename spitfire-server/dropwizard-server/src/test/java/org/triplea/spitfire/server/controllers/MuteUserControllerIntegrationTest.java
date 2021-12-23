package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

@SuppressWarnings("UnmatchedTest")
class MuteUserControllerIntegrationTest extends ControllerIntegrationTest {
  private final ModeratorChatClient client;

  MuteUserControllerIntegrationTest(final URI localhost) {
    this.client = ModeratorChatClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void muteUser() {
    client.muteUser(PlayerChatId.of("chat-id"), 600);
  }
}
