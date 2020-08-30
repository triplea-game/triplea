package org.triplea.modules.game.participants;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
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
