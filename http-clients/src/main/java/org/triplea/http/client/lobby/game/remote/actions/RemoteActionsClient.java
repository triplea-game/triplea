package org.triplea.http.client.lobby.game.remote.actions;

import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;

/**
 * Client to poll for moderator actions and to check with server for players that have been banned.
 */
// TODO: Project#12 replace stubs with implementation
public class RemoteActionsClient {

  public static RemoteActionsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new RemoteActionsClient();
  }

  // TODO: Project#12 invoke this method when new players join a hosted game-server
  @SuppressWarnings("unused")
  public boolean checkIfPlayerIsBanned(final PlayerName playerName, final String remoteIp) {
    return false;
  }

  public RemoteActions queryForRemoteActions() {
    return RemoteActions.builder().build();
  }
}
