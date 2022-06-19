package org.triplea.http.client.lobby.moderator;

import feign.RequestLine;
import java.util.List;

public interface ModeratorChatFeignClient {

  @RequestLine("POST " + ModeratorChatClient.BAN_PLAYER_PATH)
  void banPlayer(BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ModeratorChatClient.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(String value);

  @RequestLine("POST " + ModeratorChatClient.MUTE_USER)
  void mutePlayer(MuteUserRequest muteUserRequest);

  @RequestLine("POST " + ModeratorChatClient.FETCH_GAME_CHAT_HISTORY)
  List<ChatHistoryMessage> fetchChatHistoryForGame(String gameId);
}
