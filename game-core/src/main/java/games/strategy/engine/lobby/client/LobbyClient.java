package games.strategy.engine.lobby.client;

import games.strategy.net.GUID;
import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.IUserManager;

/** Provides information about a client connection to a lobby server. */
public class LobbyClient {
  @Getter private final Messengers messengers;
  private final boolean isAnonymousLogin;
  private Boolean isAdmin;

  public LobbyClient(final IMessenger messenger, final boolean anonymousLogin) {
    messengers = new Messengers(messenger);
    isAnonymousLogin = anonymousLogin;
  }

  @Nullable
  public String updatePassword(final String newPassword) {
    return getUserManager().updateUser(messengers.getLocalNode().getName(), null, newPassword);
  }

  public boolean isAdmin() {
    if (isAdmin == null) {
      final IModeratorController controller =
          (IModeratorController) messengers.getRemote(IModeratorController.REMOTE_NAME);
      isAdmin = controller.isAdmin();
    }
    return isAdmin;
  }

  public boolean isAnonymousLogin() {
    return isAnonymousLogin;
  }

  public IUserManager getUserManager() {
    return (IUserManager) messengers.getRemote(IUserManager.REMOTE_NAME);
  }

  public boolean isPasswordChangeRequired() {
    return messengers.isPasswordChangeRequired();
  }

  public Map<GUID, GameDescription> listGames() {
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

  public String getLobbyHostAddress() {
    return messengers.getRemoteServerSocketAddress().getAddress().getHostAddress();
  }

  public int getLobbyPort() {
    return messengers.getRemoteServerSocketAddress().getPort();
  }

  public IModeratorController getModeratorController() {
    return (IModeratorController) messengers.getRemote(IModeratorController.REMOTE_NAME);
  }
}
