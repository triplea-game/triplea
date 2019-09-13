package org.triplea.http.client.lobby;

import java.net.URI;
import lombok.Getter;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  // TODO: Project#12 Add additional http clients

  public HttpLobbyClient(final URI lobbyUri, final String apiKey) {
    httpModeratorToolboxClient = new HttpModeratorToolboxClient(lobbyUri, apiKey);
  }
}
