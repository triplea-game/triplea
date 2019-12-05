package org.triplea.server.lobby.chat.moderation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.server.http.AuthenticatedEndpointTest;

class ModeratorLobbyControllerIntegrationTest
    extends AuthenticatedEndpointTest<ModeratorChatClient> {

  private static final String CHAT_ID = "chat-id";

  ModeratorLobbyControllerIntegrationTest() {
    super(ModeratorChatClient::newClient);
  }

  @Test
  @DisplayName("Send ban request, verify we get a 400 for chat-id not found")
  void banPlayer() {
    final HttpInteractionException exception =
        assertThrows(HttpInteractionException.class, this::sendBanPlayerRequest);

    assertThat(exception.status(), is(400));
    assertThat(exception.getMessage(), containsString("Unable to find playerChatId: " + CHAT_ID));
  }

  private void sendBanPlayerRequest() {
    verifyEndpointReturningVoid(
        client ->
            client.banPlayer(
                BanPlayerRequest.builder().banMinutes(1).playerChatId(CHAT_ID).build()));
  }

  @Test
  @DisplayName("Send disconnect request, verify we get a 400 for chat-id not found")
  void disconnectPlayer() {
    final HttpInteractionException exception =
        assertThrows(HttpInteractionException.class, this::sendDisconnectPlayerRequest);

    assertThat(exception.status(), is(400));
    assertThat(exception.getMessage(), containsString("Unable to find playerChatId: " + CHAT_ID));
  }

  private void sendDisconnectPlayerRequest() {
    verifyEndpointReturningVoid(client -> client.disconnectPlayer(PlayerChatId.of(CHAT_ID)));
  }
}
