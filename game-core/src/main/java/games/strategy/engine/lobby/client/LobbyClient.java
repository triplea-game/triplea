package games.strategy.engine.lobby.client;

import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.lobby.common.IModeratorController;

/** Provides information about a client connection to a lobby server. */
@Getter
public class LobbyClient {
  private final Messengers messengers;
  private final boolean anonymousLogin;
  private final boolean moderator;
  private final HttpLobbyClient httpLobbyClient;

  public LobbyClient(final IMessenger messenger, final HttpLobbyClient httpLobbyClient) {
    this(messenger, httpLobbyClient, false);
  }

  public LobbyClient(
      final IMessenger messenger,
      final HttpLobbyClient httpLobbyClient,
      final boolean anonymousLogin) {
    messengers = new Messengers(messenger);
    this.httpLobbyClient = httpLobbyClient;
    this.anonymousLogin = anonymousLogin;
    final var moderatorController =
        (IModeratorController) messengers.getRemote(IModeratorController.REMOTE_NAME);
    moderator = moderatorController.isAdmin();
  }

  public boolean isPasswordChangeRequired() {
    return messengers.isPasswordChangeRequired();
  }

  /** Returns the assigned name of the current player connected to the lobby. */
  public PlayerName getPlayerName() {
    return messengers.getLocalNode().getPlayerName();
  }

  public IModeratorController getModeratorController() {
    return (IModeratorController) messengers.getRemote(IModeratorController.REMOTE_NAME);
  }
}
