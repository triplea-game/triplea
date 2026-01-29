package org.triplea.http.client.remote.actions;

import feign.RequestLine;
import java.net.InetAddress;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Client to poll for moderator actions and to check with server for players that have been banned.
 */
public interface RemoteActionsClient {
  String IS_PLAYER_BANNED_PATH = "/lobby/remote/actions/is-player-banned";
  String SEND_SHUTDOWN_PATH = "/lobby/remote/actions/send-shutdown";

  static RemoteActionsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        RemoteActionsClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + RemoteActionsClient.IS_PLAYER_BANNED_PATH)
  boolean checkIfPlayerIsBanned(String bannedIp);

  default boolean checkIfPlayerIsBanned(final InetAddress ipAddress) {
    return checkIfPlayerIsBanned(ipAddress.getHostAddress());
  }

  @RequestLine("POST " + RemoteActionsClient.SEND_SHUTDOWN_PATH)
  void sendShutdownRequest(String gameId);
}
