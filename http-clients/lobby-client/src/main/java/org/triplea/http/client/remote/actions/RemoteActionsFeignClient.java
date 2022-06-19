package org.triplea.http.client.remote.actions;

import feign.RequestLine;

interface RemoteActionsFeignClient {
  @RequestLine("POST " + RemoteActionsClient.IS_PLAYER_BANNED_PATH)
  boolean checkIfPlayerIsBanned(String bannedIp);

  @RequestLine("POST " + RemoteActionsClient.SEND_SHUTDOWN_PATH)
  void sendShutdown(String gameId);
}
