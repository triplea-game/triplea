package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(value = SpitfireServerTestExtension.LOBBY_USER_DATASET, useSequenceFiltering = false)
class GameChatHistoryControllerIntegrationTest extends ProtectedEndpointTest<ModeratorChatClient> {

  GameChatHistoryControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ModeratorChatClient::newClient);
  }

  @Test
  void fetchGameChatHistory() {
    verifyEndpoint(client -> client.fetchChatHistoryForGame("game-id"));
  }
}
