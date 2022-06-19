package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import feign.RequestLine;
import java.util.List;

interface ToolboxUserBanFeignClient {

  @RequestLine("GET " + ToolboxUserBanClient.GET_USER_BANS_PATH)
  List<UserBanData> getUserBans();

  @RequestLine("POST " + ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  void removeUserBan(String banId);

  @RequestLine("POST " + ToolboxUserBanClient.BAN_USER_PATH)
  void banUser(UserBanParams banUserParams);
}
