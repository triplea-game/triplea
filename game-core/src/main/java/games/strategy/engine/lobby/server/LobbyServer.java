package games.strategy.engine.lobby.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.util.Version;

public class LobbyServer {
  public static final String ADMIN_USERNAME = "Admin";
  public static final String LOBBY_CHAT = "_LOBBY_CHAT";
  public static final Version LOBBY_VERSION = new Version(1, 0, 0);
  private static final Logger logger = Logger.getLogger(LobbyServer.class.getName());
  private final Messengers messengers;

  private LobbyServer(final int port) {
    final IServerMessenger server;
    try {
      server = new ServerMessenger(ADMIN_USERNAME, port);
    } catch (final IOException ex) {
      logger.log(Level.SEVERE, ex.toString());
      throw new IllegalStateException(ex.getMessage());
    }
    messengers = new Messengers(server);
    server.setLoginValidator(new LobbyLoginValidator());
    // setup common objects
    new UserManager().register(messengers.getRemoteMessenger());
    final ModeratorController moderatorController = new ModeratorController(server, messengers);
    moderatorController.register(messengers.getRemoteMessenger());
    new ChatController(LOBBY_CHAT, messengers, moderatorController);

    // register the status controller
    new StatusManager(messengers).shutDown();

    final LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) messengers
        .getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
    controller.register(messengers.getRemoteMessenger());

    // now we are open for business
    server.setAcceptNewConnections(true);
  }

  /**
   * Launches a lobby instance.
   * Lobby stays running until the process is killed or the lobby is shutdown.
   */
  public static void main(final String[] args) {
    try {
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
      final int port = LobbyContext.lobbyPropertyReader().getPort();
      logger.info("Trying to listen on port:" + port);
      new LobbyServer(port);
      logger.info("Lobby started");
    } catch (final Exception ex) {
      logger.log(Level.SEVERE, ex.toString(), ex);
    }
  }

  public IServerMessenger getMessenger() {
    return (IServerMessenger) messengers.getMessenger();
  }

  public Messengers getMessengers() {
    return messengers;
  }
}
