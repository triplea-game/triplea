package games.strategy.engine.lobby.client;

import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.IUserManager;

/** Provides information about a client connection to a lobby server. */
@Getter
public class LobbyClient {
  private final Messengers messengers;
  private final boolean anonymousLogin;
  private final boolean admin;
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
    admin = moderatorController.isAdmin();
  }

  @Nullable
  public String updatePassword(final String newPassword) {
    return getUserManager().updateUser(messengers.getLocalNode().getName(), null, newPassword);
  }

  public IUserManager getUserManager() {
    return (IUserManager) messengers.getRemote(IUserManager.REMOTE_NAME);
  }

  public boolean isPasswordChangeRequired() {
    return messengers.isPasswordChangeRequired();
  }

  public Map<UUID, GameDescription> listGames() {
    return ((ILobbyGameController) messengers.getRemote(ILobbyGameController.REMOTE_NAME))
        .listGames();
  }

  public void addGameChangeListener(final ILobbyGameBroadcaster lobbyGameBroadcaster) {
    messengers.registerChannelSubscriber(lobbyGameBroadcaster, ILobbyGameBroadcaster.REMOTE_NAME);
  }

  /** Returns the assigned name of the current player connected to the lobby. */
  public String getPlayerName() {
    return messengers.getLocalNode().getName();
  }

  public IModeratorController getModeratorController() {
    return (IModeratorController) messengers.getRemote(IModeratorController.REMOTE_NAME);
  }
}
