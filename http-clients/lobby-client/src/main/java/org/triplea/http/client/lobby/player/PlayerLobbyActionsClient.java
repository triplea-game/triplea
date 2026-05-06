package org.triplea.http.client.lobby.player;

import feign.RequestLine;
import java.net.URI;
import java.util.Collection;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

/** Http client for generic actions that can be executed by a player in lobby. */
public interface PlayerLobbyActionsClient {

  static PlayerLobbyActionsClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        PlayerLobbyActionsClient.class,
        lobbyUri,
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ServerPaths.FETCH_PLAYER_INFORMATION)
  PlayerSummary fetchPlayerInformation(String playerId);

  @RequestLine("POST " + ServerPaths.FETCH_PLAYERS_IN_GAME)
  Collection<String> fetchPlayersInGame(String gameId);
}
