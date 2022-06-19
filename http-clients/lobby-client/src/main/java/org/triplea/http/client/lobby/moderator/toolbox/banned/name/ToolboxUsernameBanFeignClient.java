package org.triplea.http.client.lobby.moderator.toolbox.banned.name;

import feign.RequestLine;
import java.util.List;

interface ToolboxUsernameBanFeignClient {

  @RequestLine("POST " + ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH)
  void removeUsernameBan(String username);

  @RequestLine("POST " + ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH)
  void addUsernameBan(String username);

  @RequestLine("GET " + ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
  List<UsernameBanData> getUsernameBans();
}
