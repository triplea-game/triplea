package org.triplea.http.client.lobby.player;

import feign.RequestLine;
import java.util.Collection;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

public interface PlayerLobbyActionsFeignClient {
  @RequestLine("POST " + PlayerLobbyActionsClient.FETCH_PLAYER_INFORMATION)
  PlayerSummary fetchPlayerInformation(String value);

  @RequestLine("POST " + PlayerLobbyActionsClient.FETCH_PLAYERS_IN_GAME)
  Collection<String> fetchPlayersInGame(String gameId);
}
