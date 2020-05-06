package org.triplea.modules.moderation.mute.user;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class MuteUserControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  MuteUserControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void muteUser() {
    verifyEndpoint(client -> client.muteUser(PlayerChatId.of("chat-id"), 600));
  }
}
