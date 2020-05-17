package org.triplea.http.client.lobby.player;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Collection;
import java.util.Map;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface PlayerLobbyActionsFeignClient {
  @RequestLine("POST " + PlayerLobbyActionsClient.FETCH_PLAYER_INFORMATION)
  PlayerSummary fetchPlayerInformation(@HeaderMap Map<String, Object> headers, String value);

  @RequestLine("POST " + PlayerLobbyActionsClient.FETCH_PLAYERS_IN_GAME)
  Collection<String> fetchPlayersInGame(@HeaderMap Map<String, Object> headers, String gameId);
}
