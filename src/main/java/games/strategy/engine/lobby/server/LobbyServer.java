package games.strategy.engine.lobby.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.util.Version;

public class LobbyServer {
  private static final Logger logger = Logger.getLogger(LobbyServer.class.getName());

  public static final String TRIPLEA_LOBBY_PORT_PROPERTY = "triplea.lobby.port";
  public static final String ADMIN_USERNAME = "Admin";
  public static final String LOBBY_CHAT = "_LOBBY_CHAT";
  public static final Version LOBBY_VERSION = new Version(1, 0, 0);
  private final Messengers m_messengers;

  public static String[] getProperties() {
    return new String[] {TRIPLEA_LOBBY_PORT_PROPERTY};
  }

  /** Creates a new instance of LobbyServer. */
  public LobbyServer(final int port) {
    IServerMessenger server;
    try {
      server = new ServerMessenger(ADMIN_USERNAME, port);
    } catch (final IOException ex) {
      logger.log(Level.SEVERE, ex.toString());
      throw new IllegalStateException(ex.getMessage());
    }
    m_messengers = new Messengers(server);
    server.setLoginValidator(new LobbyLoginValidator());
    // setup common objects
    new UserManager().register(m_messengers.getRemoteMessenger());
    final ModeratorController moderatorController = new ModeratorController(server, m_messengers);
    moderatorController.register(m_messengers.getRemoteMessenger());
    new ChatController(LOBBY_CHAT, m_messengers, moderatorController);
    // register the status controller
    final StatusManager statusManager = new StatusManager(m_messengers);
    // we dont need this manager now
    statusManager.shutDown();
    final LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) m_messengers
        .getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
    controller.register(m_messengers.getRemoteMessenger());
    // now we are open for business
    server.setAcceptNewConnections(true);
  }

  /**
   * Launches a lobby instance.
   * Lobby stays running until the process is killed or the lobby is shutdown.
   */
  public static void main(final String[] args) {
    try {
      // send args to system properties
      handleCommandLineArgs(args);
      logger.info("Starting database");
      // initialize the database
      Database.getDerbyConnection().close();
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
      final int port = Integer.parseInt(System.getProperty(TRIPLEA_LOBBY_PORT_PROPERTY, "3303"));
      logger.info("Trying to listen on port:" + port);
      new LobbyServer(port);
      logger.info("Lobby started");
    } catch (final Exception ex) {
      logger.log(Level.SEVERE, ex.toString(), ex);
    }
  }

  public IServerMessenger getMessenger() {
    return (IServerMessenger) m_messengers.getMessenger();
  }

  public Messengers getMessengers() {
    return m_messengers;
  }

  private static void usage() {
    System.out.println("Arguments\n" + "   " + TRIPLEA_LOBBY_PORT_PROPERTY + "=<port number (ex: 3303)>\n");
  }
}
