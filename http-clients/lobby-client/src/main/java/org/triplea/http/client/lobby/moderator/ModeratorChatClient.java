package org.triplea.http.client.lobby.moderator;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModeratorChatClient {
  public static final String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  public static final String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";
  public static final String MUTE_USER = "/lobby/moderator/mute-player";
  public static final String FETCH_GAME_CHAT_HISTORY = "/lobby/moderator/fetch-game-chat-history";

  private ModeratorChatFeignClient moderatorLobbyFeignClient;

  public static ModeratorChatClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new ModeratorChatClient(
        HttpClient.newClient(
            ModeratorChatFeignClient.class,
            lobbyUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public void banPlayer(final BanPlayerRequest banPlayerRequest) {
    moderatorLobbyFeignClient.banPlayer(banPlayerRequest);
  }

  public void disconnectPlayer(final PlayerChatId playerChatId) {
    moderatorLobbyFeignClient.disconnectPlayer(playerChatId.getValue());
  }

  public List<ChatHistoryMessage> fetchChatHistoryForGame(final String gameId) {
    return moderatorLobbyFeignClient.fetchChatHistoryForGame(gameId);
  }

  public void muteUser(final PlayerChatId playerChatId, final long minutes) {
    moderatorLobbyFeignClient.mutePlayer(
        MuteUserRequest.builder() //
            .playerChatId(playerChatId.getValue())
            .minutes(minutes)
            .build());
  }
}
