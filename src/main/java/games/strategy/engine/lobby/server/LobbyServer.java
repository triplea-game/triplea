package games.strategy.engine.lobby.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.framework.GameRunner;
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

  public static void main(final String[] args) {
    try {
      // send args to system properties
      handleCommandLineArgs(args);
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
      final int port = Integer.parseInt(System.getProperty(TRIPLEA_LOBBY_PORT_PROPERTY, "3303"));
      logger.info("Trying to listen on port:" + port);
      new LobbyServer(port);
      logger.info("Starting database");
      // initialize the database
      Database.getConnection().close();
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

  /**
   * Move command line arguments to System.properties
   */
  private static void handleCommandLineArgs(final String[] args) {
    System.getProperties().setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    final String[] properties = getProperties();
    boolean usagePrinted = false;
    for (final String arg2 : args) {
      boolean found = false;
      String arg = arg2;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        arg = arg.substring(0, indexOf);
        for (final String propertie : properties) {
          if (arg.equals(propertie)) {
            final String value = getValue(arg2);
            System.getProperties().setProperty(propertie, value);
            System.out.println(propertie + ":" + value);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        System.out.println("Unrecogized:" + arg2);
        if (!usagePrinted) {
          usagePrinted = true;
          usage();
        }
      }
    }
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void usage() {
    System.out.println("Arguments\n" + "   " + TRIPLEA_LOBBY_PORT_PROPERTY + "=<port number (ex: 3303)>\n");
  }
}
