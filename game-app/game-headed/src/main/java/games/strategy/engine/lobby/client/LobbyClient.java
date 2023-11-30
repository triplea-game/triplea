package games.strategy.engine.lobby.client;

import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.triplea.settings.ClientSetting;
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
  private final String lobbyMessage;

  public static LobbyClient newLobbyClient(final LoginResult loginResult) {
    return LobbyClient.builder()
        .playerToLobbyConnection(
            new PlayerToLobbyConnection(
                ClientSetting.lobbyUri.getValueOrThrow(),
                loginResult.getApiKey(),
                error -> SwingComponents.showError(null, "Error communicating with lobby", error)))
        .anonymousLogin(loginResult.isAnonymousLogin())
        .moderator(loginResult.isModerator())
        .userName(loginResult.getUsername())
        .lobbyMessage(loginResult.getLoginMessage())
        .build();
  }
}
