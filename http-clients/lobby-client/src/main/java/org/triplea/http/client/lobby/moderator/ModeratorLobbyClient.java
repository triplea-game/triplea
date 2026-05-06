package org.triplea.http.client.lobby.moderator;

import feign.RequestLine;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
public interface ModeratorLobbyClient {

  static ModeratorLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ModeratorLobbyClient.class, lobbyUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ServerPaths.BAN_PLAYER_PATH)
  void banPlayer(BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ServerPaths.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(String value);

  @RequestLine("POST " + ServerPaths.MUTE_USER)
  void muteUser(MuteUserRequest muteUserRequest);

  default void muteUser(final PlayerChatId playerChatId, final long minutes) {
    muteUser(
        MuteUserRequest.builder() //
            .playerChatId(playerChatId.getValue())
            .minutes(minutes)
            .build());
  }
}
