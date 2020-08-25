package org.triplea.modules.game.participants;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

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
