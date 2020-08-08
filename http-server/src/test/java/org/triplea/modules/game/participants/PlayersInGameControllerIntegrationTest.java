package org.triplea.modules.game.participants;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class PlayersInGameControllerIntegrationTest
    extends ProtectedEndpointTest<PlayerLobbyActionsClient> {

  PlayersInGameControllerIntegrationTest() {
    super(AllowedUserRole.ANONYMOUS, PlayerLobbyActionsClient::new);
  }

  @Test
  void fetchPlayerInfo() {
    verifyEndpoint(client -> client.fetchPlayersInGame("game-id"));
  }
}
