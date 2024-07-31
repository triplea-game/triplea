package org.triplea.http.client.lobby.moderator;

import feign.RequestLine;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
public interface ModeratorLobbyClient {
  String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";
  String MUTE_USER = "/lobby/moderator/mute-player";

  static ModeratorLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ModeratorLobbyClient.class, lobbyUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ModeratorLobbyClient.BAN_PLAYER_PATH)
  void banPlayer(BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ModeratorLobbyClient.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(String value);

  @RequestLine("POST " + ModeratorLobbyClient.MUTE_USER)
  void muteUser(MuteUserRequest muteUserRequest);

  default void muteUser(final PlayerChatId playerChatId, final long minutes) {
    muteUser(
        MuteUserRequest.builder() //
            .playerChatId(playerChatId.getValue())
            .minutes(minutes)
            .build());
  }
}
