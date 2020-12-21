package org.triplea.modules.moderation.mute.user;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
class MuteUserControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  MuteUserControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void muteUser() {
    verifyEndpoint(client -> client.muteUser(PlayerChatId.of("chat-id"), 600));
  }
}
