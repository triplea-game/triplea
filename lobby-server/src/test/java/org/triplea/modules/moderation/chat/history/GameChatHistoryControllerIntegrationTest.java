package org.triplea.modules.moderation.chat.history;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class GameChatHistoryControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  GameChatHistoryControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void fetchGameChatHistory() {
    verifyEndpoint(client -> client.fetchChatHistoryForGame("game-id"));
  }
}
