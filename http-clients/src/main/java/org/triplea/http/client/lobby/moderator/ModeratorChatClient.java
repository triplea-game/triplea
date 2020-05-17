package org.triplea.http.client.lobby.moderator;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModeratorChatClient {
  public static final String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  public static final String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";
  public static final String MUTE_USER = "/lobby/moderator/mute-player";
  public static final String FETCH_PLAYER_INFORMATION = "/lobby/moderator/fetch-player-info";
  public static final String FETCH_GAME_CHAT_HISTORY = "/lobby/moderator/fetch-game-chat-history";

  private AuthenticationHeaders authenticationHeaders;
  private ModeratorChatFeignClient moderatorLobbyFeignClient;

  public static ModeratorChatClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new ModeratorChatClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ModeratorChatFeignClient.class, lobbyUri).get());
  }

  public void banPlayer(final BanPlayerRequest banPlayerRequest) {
    moderatorLobbyFeignClient.banPlayer(authenticationHeaders.createHeaders(), banPlayerRequest);
  }

  public void disconnectPlayer(final PlayerChatId playerChatId) {
    moderatorLobbyFeignClient.disconnectPlayer(
        authenticationHeaders.createHeaders(), playerChatId.getValue());
  }

  public PlayerSummary fetchPlayerInformation(final PlayerChatId playerChatId) {
    return moderatorLobbyFeignClient.fetchPlayerInformation(
        authenticationHeaders.createHeaders(), playerChatId.getValue());
  }

  public List<ChatHistoryMessage> fetchChatHistoryForGame(final String gameId) {
    return moderatorLobbyFeignClient.fetchChatHistoryForGame(
        authenticationHeaders.createHeaders(), gameId);
  }

  public void muteUser(final PlayerChatId playerChatId, final long minutes) {
    moderatorLobbyFeignClient.mutePlayer(
        authenticationHeaders.createHeaders(),
        MuteUserRequest.builder() //
            .playerChatId(playerChatId.getValue())
            .minutes(minutes)
            .build());
  }
}
