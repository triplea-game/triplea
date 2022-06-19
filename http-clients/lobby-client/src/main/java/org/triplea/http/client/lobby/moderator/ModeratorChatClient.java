package org.triplea.http.client.lobby.moderator;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Provides access to moderator lobby commands, such as 'disconnect' player, and 'ban' player. */
public interface ModeratorChatClient {
  String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";
  String MUTE_USER = "/lobby/moderator/mute-player";
  String FETCH_GAME_CHAT_HISTORY = "/lobby/moderator/fetch-game-chat-history";

  static ModeratorChatClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ModeratorChatClient.class, lobbyUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ModeratorChatClient.BAN_PLAYER_PATH)
  void banPlayer(BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ModeratorChatClient.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(String value);

  @RequestLine("POST " + ModeratorChatClient.FETCH_GAME_CHAT_HISTORY)
  List<ChatHistoryMessage> fetchChatHistoryForGame(String gameId);

  @RequestLine("POST " + ModeratorChatClient.MUTE_USER)
  void muteUser(MuteUserRequest muteUserRequest);

  default void muteUser(final PlayerChatId playerChatId, final long minutes) {
    muteUser(
        MuteUserRequest.builder() //
            .playerChatId(playerChatId.getValue())
            .minutes(minutes)
            .build());
  }
}
