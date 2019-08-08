package games.strategy.engine.lobby.client;

import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;
import lombok.Getter;
import org.triplea.lobby.common.IModeratorController;

/** Provides information about a client connection to a lobby server. */
public class LobbyClient {
  @Getter private final Messengers messengers;
  private final boolean isAnonymousLogin;
  private Boolean isAdmin;

  public LobbyClient(final IMessenger messenger, final boolean anonymousLogin) {
    messengers = new Messengers(messenger);
    isAnonymousLogin = anonymousLogin;
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
}
