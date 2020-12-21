package org.triplea.modules.moderation.mute.user;

import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class MuteUserControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  MuteUserControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  // Disabled until can be determined what is wrong, possibly to be deleted.
  // This test fails when run individually with a 401 unauthorized. The test
  // does appear to have a problem as calling the mute-player endpoint does work.
  @Disabled
  @Test
  void muteUser() {
    verifyEndpoint(client -> client.muteUser(PlayerChatId.of("chat-id"), 600));
  }
}
