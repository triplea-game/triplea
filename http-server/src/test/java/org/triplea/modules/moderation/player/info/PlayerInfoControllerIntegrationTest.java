package org.triplea.modules.moderation.player.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class PlayerInfoControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  PlayerInfoControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  @DisplayName("Verify fetch player info endpoint is available")
  void fetchPlayerInfo() {
    try {
      verifyEndpoint(client -> client.fetchPlayerInformation(PlayerChatId.of("id")));
    } catch (final HttpInteractionException expected) {
      assertThat(
          "The requested player ID does not exist, we expect a 400 back for this request",
          expected.status(),
          is(400));
    }
  }
}
