package org.triplea.modules.moderation.disconnect.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
class DisconnectUserControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  private static final PlayerChatId CHAT_ID = PlayerChatId.of("chat-id");

  DisconnectUserControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  @DisplayName("Send disconnect request, verify we get a 400 for chat-id not found")
  void disconnectPlayer() {
    final HttpInteractionException exception =
        assertThrows(
            HttpInteractionException.class,
            () -> verifyEndpoint(client -> client.disconnectPlayer(CHAT_ID)));

    assertThat(exception.status(), is(400));
  }
}
