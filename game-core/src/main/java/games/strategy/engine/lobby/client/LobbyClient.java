package games.strategy.engine.lobby.client;

import org.triplea.lobby.common.IModeratorController;

import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;

/**
 * Provides information about a client connection to a lobby server.
 */
public class LobbyClient {
  private final Messengers messengers;
  private final boolean isAnonymousLogin;
  private Boolean isAdmin;

  public LobbyClient(final IMessenger messenger, final boolean anonymousLogin) {
    messengers = new Messengers(messenger);
    isAnonymousLogin = anonymousLogin;
  }

  public boolean isAdmin() {
    if (isAdmin == null) {
      final IModeratorController controller = (IModeratorController) messengers.getRemoteMessenger()
          .getRemote(IModeratorController.REMOTE_NAME);
      isAdmin = controller.isAdmin();
    }
    return isAdmin;
  }

  public boolean isAnonymousLogin() {
    return isAnonymousLogin;
  }

  public IChannelMessenger getChannelMessenger() {
    return messengers.getChannelMessenger();
  }

  public IMessenger getMessenger() {
    return messengers.getMessenger();
  }

  public IRemoteMessenger getRemoteMessenger() {
    return messengers.getRemoteMessenger();
  }

  public Messengers getMessengers() {
    return messengers;
  }
}
