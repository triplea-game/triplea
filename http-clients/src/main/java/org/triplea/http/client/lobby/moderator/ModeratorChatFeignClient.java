package org.triplea.http.client.lobby.moderator;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface ModeratorChatFeignClient {

  @RequestLine("POST " + ModeratorChatClient.BAN_PLAYER_PATH)
  void banPlayer(@HeaderMap Map<String, Object> headers, BanPlayerRequest banPlayerRequest);

  @RequestLine("POST " + ModeratorChatClient.DISCONNECT_PLAYER_PATH)
  void disconnectPlayer(@HeaderMap Map<String, Object> headers, String value);

  @RequestLine("POST " + ModeratorChatClient.FETCH_PLAYER_INFORMATION)
  PlayerSummaryForModerator fetchPlayerInformation(
      @HeaderMap Map<String, Object> headers, String value);

  @RequestLine("POST " + ModeratorChatClient.MUTE_USER)
  void mutePlayer(@HeaderMap Map<String, Object> headers, MuteUserRequest muteUserRequest);

  @RequestLine("POST " + ModeratorChatClient.FETCH_GAME_CHAT_HISTORY)
  List<ChatHistoryMessage> fetchChatHistoryForGame(
      @HeaderMap Map<String, Object> headers, String gameId);
}
