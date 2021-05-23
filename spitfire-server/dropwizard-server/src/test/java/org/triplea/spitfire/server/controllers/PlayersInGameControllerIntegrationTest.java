package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(value = SpitfireServerTestExtension.LOBBY_USER_DATASET, useSequenceFiltering = false)
class PlayersInGameControllerIntegrationTest
    extends ProtectedEndpointTest<PlayerLobbyActionsClient> {

  PlayersInGameControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.ANONYMOUS, PlayerLobbyActionsClient::new);
  }

  @Test
  void fetchPlayerInfo() {
    verifyEndpoint(client -> client.fetchPlayersInGame("game-id"));
  }
}
