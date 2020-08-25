package org.triplea.modules.moderation.mute.user;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class MuteUserControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  MuteUserControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void muteUser() {
    verifyEndpoint(client -> client.muteUser(PlayerChatId.of("chat-id"), 600));
  }
}
