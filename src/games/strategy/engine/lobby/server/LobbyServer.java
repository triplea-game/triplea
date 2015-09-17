package games.strategy.engine.lobby.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.google.common.base.Throwables;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.lobby.server.headless.HeadlessLobbyConsole;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.ui.LobbyAdminConsole;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.Version;


public class LobbyServer {
  public static final String ADMIN_USERNAME = "Admin";
  public static final String LOBBY_CHAT = "_LOBBY_CHAT";
  public static final Version LOBBY_VERSION = new Version(1, 0, 0);
  public static final int DEFAULT_LOBBY_PORT = 3303;

  private static final String LOBBY_PORT_PROPERTY = "triplea.lobby.port";
  private static final String ENABLE_UI_PROPERTY = "triplea.lobby.ui";
  private static final String ENABLE_CONSOLE_PROPERTY = "triplea.lobby.console";
  private static LobbyServer runningServer = null;

  private final static Logger s_logger = Logger.getLogger(LobbyServer.class.getName());
  private final Messengers m_messengers;
  private final IServerMessenger server;


  public static void main(final String args[]) {
    setSystemProperties(args);
    try {
      System.out.println("Initializating the database");
      Database.getConnection().close();
    } catch (final Exception ex) {
      s_logger.log(Level.SEVERE, "Failed to initialize DB, failed to start the lobby. " + ex.toString(), ex);
      return;
    }

    final int port = Integer.parseInt(System.getProperty(LOBBY_PORT_PROPERTY, String.valueOf(DEFAULT_LOBBY_PORT)));
    System.out.println("Trying to listen on port:" + port);
    ServerMessenger serverMessenger = createServerMessenger(port);
    runLobby(serverMessenger);
  }

  protected static void runLobby(ServerMessenger serverMessenger) {
    runningServer = new LobbyServer(serverMessenger);

    final boolean startUI = Boolean.parseBoolean(System.getProperty(ENABLE_UI_PROPERTY, "false"));
    if (startUI) {
      startUI(runningServer);
    } else {
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
    }
    if (Boolean.parseBoolean(System.getProperty(ENABLE_CONSOLE_PROPERTY, "false"))) {
      System.out.println("starting console");
      new HeadlessLobbyConsole(runningServer, System.in, System.out).start();
    }
    setUpLoggingOnStandardInputOutputStreams();
    s_logger.info("Lobby started");
  }


  protected static ServerMessenger createServerMessenger(final int port) {
    try {
      return new ServerMessenger(ADMIN_USERNAME, port);
    } catch (final IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static String[] getProperties() {
    return new String[] {LOBBY_PORT_PROPERTY, ENABLE_CONSOLE_PROPERTY, ENABLE_UI_PROPERTY};
  }



  private static void setUpLoggingOnStandardInputOutputStreams() {
    // setup logging to read our logging.properties
    try {
      LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("server-logging.properties"));
    } catch (final Exception e) {
      e.printStackTrace();
    }
    Logger.getAnonymousLogger().info("Redirecting std out");
    System.setErr(new LoggingPrintStream("ERROR", Level.SEVERE));
    System.setOut(new LoggingPrintStream("OUT", Level.INFO));
  }

  /**
   * Move command line arguments to System.properties
   */
  private static void setSystemProperties(final String[] args) {
    System.getProperties().setProperty(HeadlessGameServer.TRIPLEA_HEADLESS, "true");
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
    System.out.println("Arguments\n" + "   " + LOBBY_PORT_PROPERTY + "=<port number (ex: " + DEFAULT_LOBBY_PORT + ")>\n" + "   "
        + ENABLE_UI_PROPERTY + "=<true/false>\n" + "   " + ENABLE_CONSOLE_PROPERTY + "=<true/false>\n");
  }


  private static void startUI(final LobbyServer server) {
    System.out.println("starting ui");
    final LobbyAdminConsole console = new LobbyAdminConsole(server);
    console.setSize(800, 700);
    console.setLocationRelativeTo(null);
    console.setVisible(true);
  }

  protected static void stopServer() {
    if( runningServer != null ) {
      runningServer.stop();
    }
  }


  /** Creates a new instance of LobbyServer */
  public LobbyServer(final ServerMessenger serverMessenger ) {
    server = serverMessenger;

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

  private void stop() {
    server.shutDown();
  }

  public IServerMessenger getMessenger() {
    return (IServerMessenger) m_messengers.getMessenger();
  }

  public Messengers getMessengers() {
    return m_messengers;
  }
}
