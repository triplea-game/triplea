package org.triplea.http.client.remote.actions;

import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Client to poll for moderator actions and to check with server for players that have been banned.
 */
@Log
public class RemoteActionsClient {
  public static final String IS_PLAYER_BANNED_PATH = "/remote/actions/is-player-banned";
  public static final String SEND_SHUTDOWN_PATH = "/remote/actions/send-shutdown";

  private final RemoteActionsFeignClient remoteActionsFeignClient;

  private final Map<String, Object> headers;

  public RemoteActionsClient(final URI serverUri, final ApiKey apiKey) {
    remoteActionsFeignClient = new HttpClient<>(RemoteActionsFeignClient.class, serverUri).get();
    headers = new AuthenticationHeaders(apiKey).createHeaders();
  }

  // TODO: Project#12 invoke this method when new players join a hosted game-server
  public boolean checkIfPlayerIsBanned(final InetAddress ipAddress) {
    return remoteActionsFeignClient.checkIfPlayerIsBanned(headers, ipAddress.getHostAddress());
  }

  // TODO: Project#12 wire this to moderator 'shutdown action'
  public void sendShutdownRequest(final InetAddress ipAddress) {
    remoteActionsFeignClient.sendShutdown(headers, ipAddress.getHostAddress());
  }
}
