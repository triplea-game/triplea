package org.triplea.http.client.lobby.moderator;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

// TODO: Project#12 test-me
/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModeratorLobbyClient {

  public static final String DISCONNECT_GAME_PATH = "/lobby/moderator/disconnect-game";
  public static final String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  public static final String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";

  private AuthenticationHeaders authenticationHeaders;
  private ModeratorLobbyFeignClient moderatorLobbyFeignClient;

  public static ModeratorLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new ModeratorLobbyClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ModeratorLobbyFeignClient.class, lobbyUri).get());
  }

  public void banPlayer(final BanPlayerRequest banPlayerRequest) {
    moderatorLobbyFeignClient.banPlayer(authenticationHeaders.createHeaders(), banPlayerRequest);
  }

  public void disconnectPlayer(final PlayerChatId playerChatId) {
    moderatorLobbyFeignClient.disconnectPlayer(
        authenticationHeaders.createHeaders(), playerChatId.getValue());
  }

  public void disconnectGame(final String gameId) {
    moderatorLobbyFeignClient.disconnectGame(authenticationHeaders.createHeaders(), gameId);
  }
}
