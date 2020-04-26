package org.triplea.http.client.lobby;

import java.net.URI;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.upload.ChatUploadClient;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.http.client.remote.actions.RemoteActionsClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final URI lobbyUri;
  private final ApiKey apiKey;

  private final ConnectivityCheckClient connectivityCheckClient;
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final ModeratorChatClient moderatorLobbyClient;
  private final UserAccountClient userAccountClient;
  private final RemoteActionsClient remoteActionsClient;
  private final ChatUploadClient chatUploadClient;

  private HttpLobbyClient(final URI lobbyUri, final ApiKey apiKey) {
    this.lobbyUri = lobbyUri;
    this.apiKey = apiKey;

    connectivityCheckClient = ConnectivityCheckClient.newClient(lobbyUri, apiKey);
    httpModeratorToolboxClient = HttpModeratorToolboxClient.newClient(lobbyUri, apiKey);
    moderatorLobbyClient = ModeratorChatClient.newClient(lobbyUri, apiKey);
    userAccountClient = UserAccountClient.newClient(lobbyUri, apiKey);
    remoteActionsClient = new RemoteActionsClient(lobbyUri, apiKey);
    chatUploadClient = ChatUploadClient.newClient(lobbyUri, apiKey);
  }

  public GameListingClient newGameListingClient() {
    return GameListingClient.newClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new HttpLobbyClient(lobbyUri, apiKey);
  }
}
