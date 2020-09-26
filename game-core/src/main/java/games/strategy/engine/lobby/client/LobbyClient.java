package games.strategy.engine.lobby.client;

import games.strategy.engine.lobby.client.login.LoginResult;
import java.net.URI;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.SwingComponents;

/** Provides information about a client connection to a lobby server. */
@Getter
@Builder
public class LobbyClient {
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final UserName userName;
  private final boolean anonymousLogin;
  private final boolean moderator;
  private final boolean passwordChangeRequired;

  public static LobbyClient newLobbyClient(final URI lobbyUri, final LoginResult loginResult) {
    return LobbyClient.builder()
        .playerToLobbyConnection(
            new PlayerToLobbyConnection(
                lobbyUri,
                loginResult.getApiKey(),
                error -> SwingComponents.showError(null, "Error communicating with lobby", error)))
        .anonymousLogin(loginResult.isAnonymousLogin())
        .moderator(loginResult.isModerator())
        .userName(loginResult.getUsername())
        .build();
  }
}
