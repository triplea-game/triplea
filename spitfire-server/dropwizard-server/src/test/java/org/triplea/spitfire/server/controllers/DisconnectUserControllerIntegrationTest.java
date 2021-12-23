package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

@SuppressWarnings("UnmatchedTest")
class DisconnectUserControllerIntegrationTest extends ControllerIntegrationTest {
  private static final PlayerChatId CHAT_ID = PlayerChatId.of("chat-id");
  private final URI localhost;
  private final ModeratorChatClient client;

  DisconnectUserControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    this.client = ModeratorChatClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ModeratorChatClient.newClient(localhost, apiKey),
        client -> client.disconnectPlayer(PlayerChatId.of("chat-id")));
  }

  @Test
  @DisplayName("Send disconnect request, verify we get a 400 for chat-id not found")
  void disconnectPlayer() {
    assertBadRequest(() -> client.disconnectPlayer(CHAT_ID));
  }
}
