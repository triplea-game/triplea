package org.triplea.modules.moderation.chat.history;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
class GameChatHistoryControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  GameChatHistoryControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void fetchGameChatHistory() {
    verifyEndpoint(client -> client.fetchChatHistoryForGame("game-id"));
  }
}
