package org.triplea.http.client.remote.actions;

import java.net.InetAddress;
import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Client to poll for moderator actions and to check with server for players that have been banned.
 */
public class RemoteActionsClient {
  public static final String IS_PLAYER_BANNED_PATH = "/remote/actions/is-player-banned";
  public static final String SEND_SHUTDOWN_PATH = "/remote/actions/send-shutdown";

  private final RemoteActionsFeignClient remoteActionsFeignClient;

  public RemoteActionsClient(final URI serverUri, final ApiKey apiKey) {
    remoteActionsFeignClient =
        HttpClient.newClient(
            RemoteActionsFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders());
  }

  public static RemoteActionsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new RemoteActionsClient(serverUri, apiKey);
  }

  public boolean checkIfPlayerIsBanned(final InetAddress ipAddress) {
    return remoteActionsFeignClient.checkIfPlayerIsBanned(ipAddress.getHostAddress());
  }

  public void sendShutdownRequest(final String gameId) {
    remoteActionsFeignClient.sendShutdown(gameId);
  }
}
