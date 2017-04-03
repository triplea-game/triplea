package games.strategy.engine.framework.headlessGameServer;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.engine.ClientContext;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.Constants;
import games.strategy.util.MD5Crypt;
import games.strategy.util.ThreadUtil;
import games.strategy.util.TimeManager;

/**
 * A way of hosting a game, but headless.
 */
public class HeadlessGameServer {

  static final Logger s_logger = Logger.getLogger(HeadlessGameServer.class.getName());
  static HeadlessGameServerConsole s_console = null;
  private static HeadlessGameServer s_instance = null;
  private final AvailableGames m_availableGames;
  private final GameSelectorModel m_gameSelectorModel;
  private SetupPanelModel m_setupPanelModel = null;
  private final ScheduledExecutorService m_lobbyWatcherResetupThread = Executors.newScheduledThreadPool(1);
  private ServerGame m_iGame = null;
  private boolean m_shutDown = false;
  private final String m_startDate = TimeManager.getGMTString(new Date());

  public static synchronized HeadlessGameServer getInstance() {
    return s_instance;
  }

  public static synchronized boolean headless() {
    if (getInstance() != null) {
      return true;
    }
    return Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"));
  }

  public Set<String> getAvailableGames() {
    return new HashSet<>(m_availableGames.getGameNames());
  }

  public synchronized void setGameMapTo(final String gameName) {
    // don't change mid-game
    if (m_setupPanelModel.getPanel() != null && m_iGame == null) {
      if (!m_availableGames.getGameNames().contains(gameName)) {
        return;
      }
      m_gameSelectorModel.load(m_availableGames.getGameData(gameName), m_availableGames.getGameFilePath(gameName));
      System.out.println("Changed to game map: " + gameName);
    }
  }

  public synchronized void loadGameSave(final File file) {
    // don't change mid-game
    if (m_setupPanelModel.getPanel() != null && m_iGame == null) {
      if (file == null || !file.exists()) {
        return;
      }
      m_gameSelectorModel.load(file, null);
      System.out.println("Changed to save: " + file.getName());
    }
  }

  public synchronized void loadGameSave(final InputStream input, final String fileName) {
    // don't change mid-game
    if (m_setupPanelModel.getPanel() != null && m_iGame == null) {
      if (input == null || fileName == null) {
        return;
      }
      final GameData data = m_gameSelectorModel.getGameData(input);
      if (data == null) {
        System.out.println("Loading GameData failed for: " + fileName);
        return;
      }
      final String mapNameProperty = data.getProperties().get(Constants.MAP_NAME, "");
      Set<String> availableMaps = m_availableGames.getAvailableMapFolderOrZipNames();
      if (!availableMaps.contains(mapNameProperty) && !availableMaps.contains(mapNameProperty + "-master")) {
        System.out.println("Game mapName not in available games listing: " + mapNameProperty);
        return;
      }
      m_gameSelectorModel.load(data, fileName);
      System.out.println("Changed to user savegame: " + fileName);
    }
  }

  public synchronized void loadGameOptions(final byte[] bytes) {
    // don't change mid-game
    if (m_setupPanelModel.getPanel() != null && m_iGame == null) {
      if (bytes == null || bytes.length == 0) {
        return;
      }
      final GameData data = m_gameSelectorModel.getGameData();
      if (data == null) {
        return;
      }
      final GameProperties props = data.getProperties();
      if (props == null) {
        return;
      }
      GameProperties.applyByteMapToChangeProperties(bytes, props);
      System.out.println("Changed to user game options.");
    }
  }

  public static synchronized void setServerGame(final ServerGame serverGame) {
    final HeadlessGameServer instance = getInstance();
    if (instance != null) {
      instance.m_iGame = serverGame;
      if (serverGame != null) {
        System.out.println("Game starting up: " + instance.m_iGame.isGameSequenceRunning() + ", GameOver: "
            + instance.m_iGame.isGameOver() + ", Players: " + instance.m_iGame.getPlayerManager().toString());
      }
    }
  }

  public static synchronized void log(final String stdout) {
    final HeadlessGameServer instance = getInstance();
    if (instance != null) {
      System.out.println(stdout);
    }
  }

  public static synchronized void sendChat(final String chatString) {
    final HeadlessGameServer instance = getInstance();
    if (instance != null) {
      final Chat chat = instance.getChat();
      if (chat != null) {
        try {
          chat.sendMessage(chatString, false);
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      }
    }
  }

  public String getSalt() {
    final String encryptedPassword = MD5Crypt.crypt(System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, ""));
    final String salt = MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword);
    return salt;
  }

  public String remoteShutdown(final String hashedPassword, final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      (new Thread(() -> {
        System.out.println("Remote Shutdown Initiated.");
        System.exit(0);
      })).start();
      return null;
    }
    System.out.println("Attempted remote shutdown with invalid password.");
    return "Invalid password!";
  }

  public String remoteStopGame(final String hashedPassword, final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      final ServerGame iGame = m_iGame;
      if (iGame != null) {
        (new Thread(() -> {
          System.out.println("Remote Stop Game Initiated.");
          SaveGameFileChooser.ensureMapsFolderExists();
          final File f1 =
              new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSaveFileName());
          final File f2 =
              new File(ClientContext.folderSettings().getSaveGamePath(), SaveGameFileChooser.getAutoSave2FileName());
          final File f;
          if (f1.lastModified() > f2.lastModified()) {
            f = f2;
          } else {
            f = f1;
          }
          try {
            iGame.saveGame(f);
          } catch (final Exception e) {
            ClientLogger.logQuietly(e);
          }
          iGame.stopGame();
        })).start();
      }
      return null;
    }
    System.out.println("Attempted remote stop game with invalid password.");
    return "Invalid password!";
  }

  public String remoteGetChatLog(final String hashedPassword, final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      final IChatPanel chat = getServerModel().getChatPanel();
      if (chat == null || chat.getAllText() == null) {
        return "Empty or null chat";
      }
      return chat.getAllText();
    }
    System.out.println("Attempted remote get chat log with invalid password.");
    return "Invalid password!";
  }

  public String remoteMutePlayer(final String playerName, final int minutes, final String hashedPassword,
      final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    // milliseconds (48 hours max)
    final long expire = System.currentTimeMillis() + (Math.max(0, Math.min(60 * 24 * 2, minutes)) * 1000 * 60);
    if (encryptedPassword.equals(hashedPassword)) {
      (new Thread(() -> {
        if (getServerModel() == null) {
          return;
        }
        final IServerMessenger messenger = getServerModel().getMessenger();
        if (messenger == null) {
          return;
        }
        final Set<INode> nodes = messenger.getNodes();
        if (nodes == null) {
          return;
        }
        try {
          for (final INode node : nodes) {
            final String realName = node.getName().split(" ")[0];
            final String ip = node.getAddress().getHostAddress();
            final String mac = messenger.getPlayerMac(node.getName());
            if (realName.equals(playerName)) {
              System.out.println("Remote Mute of Player: " + playerName);
              messenger.NotifyUsernameMutingOfPlayer(realName, new Date(expire));
              messenger.NotifyIPMutingOfPlayer(ip, new Date(expire));
              messenger.NotifyMacMutingOfPlayer(mac, new Date(expire));
              return;
            }
          }
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      })).start();
      return null;
    }
    System.out.println("Attempted remote mute player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBootPlayer(final String playerName, final String hashedPassword, final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      (new Thread(() -> {
        if (getServerModel() == null) {
          return;
        }
        final IServerMessenger messenger = getServerModel().getMessenger();
        if (messenger == null) {
          return;
        }
        final Set<INode> nodes = messenger.getNodes();
        if (nodes == null) {
          return;
        }
        try {
          for (final INode node : nodes) {
            final String realName = node.getName().split(" ")[0];
            if (realName.equals(playerName)) {
              System.out.println("Remote Boot of Player: " + playerName);
              messenger.removeConnection(node);
            }
          }
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      })).start();
      return null;
    }
    System.out.println("Attempted remote boot player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBanPlayer(final String playerName, final int hours, final String hashedPassword,
      final String salt) {
    final String password = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
    // milliseconds (30 days max)
    final long expire = System.currentTimeMillis() + (Math.max(0, Math.min(24 * 30, hours)) * 1000 * 60 * 60);
    if (encryptedPassword.equals(hashedPassword)) {
      (new Thread(() -> {
        if (getServerModel() == null) {
          return;
        }
        final IServerMessenger messenger = getServerModel().getMessenger();
        if (messenger == null) {
          return;
        }
        final Set<INode> nodes = messenger.getNodes();
        if (nodes == null) {
          return;
        }
        try {
          for (final INode node : nodes) {
            final String realName = node.getName().split(" ")[0];
            final String ip = node.getAddress().getHostAddress();
            final String mac = messenger.getPlayerMac(node.getName());
            if (realName.equals(playerName)) {
              System.out.println("Remote Ban of Player: " + playerName);
              try {
                messenger.NotifyUsernameMiniBanningOfPlayer(realName, new Date(expire));
              } catch (final Exception e) {
                ClientLogger.logQuietly(e);
              }
              try {
                messenger.NotifyIPMiniBanningOfPlayer(ip, new Date(expire));
              } catch (final Exception e) {
                ClientLogger.logQuietly(e);
              }
              try {
                messenger.NotifyMacMiniBanningOfPlayer(mac, new Date(expire));
              } catch (final Exception e) {
                ClientLogger.logQuietly(e);
              }
              messenger.removeConnection(node);
            }
          }
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
      })).start();
      return null;
    }
    System.out.println("Attempted remote ban player with invalid password.");
    return "Invalid password!";
  }

  ServerGame getIGame() {
    return m_iGame;
  }

  public boolean isShutDown() {
    return m_shutDown;
  }

  public HeadlessGameServer() {
    super();
    if (s_instance != null) {
      throw new IllegalStateException("Instance already exists");
    }
    s_instance = this;
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Running ShutdownHook.");
      shutdown();
    }));
    m_availableGames = new AvailableGames();
    m_gameSelectorModel = new GameSelectorModel();
    final String fileName = System.getProperty(GameRunner.TRIPLEA_GAME_PROPERTY, "");
    if (fileName.length() > 0) {
      try {
        final File file = new File(fileName);
        m_gameSelectorModel.load(file, null);
      } catch (final Exception e) {
        m_gameSelectorModel.resetGameDataToNull();
      }
    }
    final Runnable r = () -> {
      System.out.println("Headless Start");
      m_setupPanelModel = new HeadlessServerSetupPanelModel(m_gameSelectorModel, null);
      m_setupPanelModel.showSelectType();
      System.out.println("Waiting for users to connect.");
      waitForUsersHeadless();
    };
    final Thread t = new Thread(r, "Initialize Headless Server Setup Model");
    t.start();

    int reconnect;
    try {
      final String reconnectionSeconds =
          System.getProperty(GameRunner.LOBBY_GAME_RECONNECTION, "" + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
      reconnect = Math.max(Integer.parseInt(reconnectionSeconds), GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM);
    } catch (final NumberFormatException e) {
      reconnect = GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT;
    }
    m_lobbyWatcherResetupThread.scheduleAtFixedRate(() -> {
      try {
        restartLobbyWatcher(m_setupPanelModel, m_iGame);
      } catch (final Exception e) {
        ThreadUtil.sleep(10 * 60 * 1000);
        // try again, but don't catch it this time
        restartLobbyWatcher(m_setupPanelModel, m_iGame);
      }
    }, reconnect, reconnect, TimeUnit.SECONDS);
    s_logger.info("Game Server initialized");
  }

  private static synchronized void restartLobbyWatcher(final SetupPanelModel setupPanelModel, final ServerGame iGame) {
    try {
      final ISetupPanel setup = setupPanelModel.getPanel();
      if (setup == null) {
        return;
      }
      if (iGame != null) {
        return;
      }
      if (setup.canGameStart()) {
        return;
      }
      if (setup instanceof ServerSetupPanel) {
        ((ServerSetupPanel) setup).repostLobbyWatcher(iGame);
      } else if (setup instanceof HeadlessServerSetup) {
        ((HeadlessServerSetup) setup).repostLobbyWatcher(iGame);
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  public static void resetLobbyHostOldExtensionProperties() {
    for (final String property : getProperties()) {
      if (GameRunner.LOBBY_HOST.equals(property) || LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY.equals(property)
          || GameRunner.LOBBY_GAME_HOSTED_BY.equals(property)) {
        // for these 3 properties, we clear them after hosting, but back them up.
        final String oldValue = System.getProperty(property + GameRunner.OLD_EXTENSION);
        if (oldValue != null) {
          System.setProperty(property, oldValue);
        }
      }
    }
  }

  public static String[] getProperties() {
    return new String[] {GameRunner.TRIPLEA_GAME_PROPERTY, GameRunner.TRIPLEA_GAME_HOST_CONSOLE_PROPERTY,
        GameRunner.TRIPLEA_SERVER_PROPERTY, GameRunner.TRIPLEA_PORT_PROPERTY,
        GameRunner.TRIPLEA_NAME_PROPERTY, GameRunner.LOBBY_HOST, LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY,
        GameRunner.LOBBY_GAME_COMMENTS, GameRunner.LOBBY_GAME_HOSTED_BY, GameRunner.LOBBY_GAME_SUPPORT_EMAIL,
        GameRunner.LOBBY_GAME_SUPPORT_PASSWORD, GameRunner.LOBBY_GAME_RECONNECTION,
        GameRunner.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, GameRunner.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,
        GameRunner.MAP_FOLDER};
  }

  public String getStatus() {
    String message = "Server Start Date: " + m_startDate;
    final ServerGame game = getIGame();
    if (game != null) {
      message += "\nIs currently running: " + game.isGameSequenceRunning() + "\nIs GameOver: " + game.isGameOver()
          + "\nGame: " + game.getData().getGameName() + "\nRound: " + game.getData().getSequence().getRound()
          + "\nPlayers: " + game.getPlayerManager().toString();
    } else {
      message += "\nCurrently Waiting To Start A Game";
    }
    return message;
  }

  public void printThreadDumpsAndStatus() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Dump to Log:");
    sb.append("\n\nStatus:\n");
    sb.append(getStatus());
    sb.append("\n\nServer:\n");
    sb.append(getServerModel());
    sb.append("\n\n");
    sb.append(DebugUtils.getThreadDumps());
    sb.append("\n\n");
    sb.append(DebugUtils.getMemory());
    sb.append("\n\nDump finished.\n");
    System.out.println(sb.toString());
  }

  public synchronized void shutdown() {
    m_shutDown = true;
    printThreadDumpsAndStatus();
    try {
      if (m_lobbyWatcherResetupThread != null) {
        m_lobbyWatcherResetupThread.shutdown();
      }
    } catch (final Exception e) {
    }
    try {
      if (m_iGame != null) {
        m_iGame.stopGame();
      }
    } catch (final Exception e) {
    }
    try {
      if (m_setupPanelModel != null) {
        final ISetupPanel setup = m_setupPanelModel.getPanel();
        if (setup != null && setup instanceof ServerSetupPanel) {
          // this is causing a deadlock when in a shutdown hook, due to swing/awt
          // ((ServerSetupPanel) setup).shutDown();
        } else if (setup != null && setup instanceof HeadlessServerSetup) {
          setup.shutDown();
        }
      }
    } catch (final Exception e) {
    }
    try {
      if (m_gameSelectorModel != null && m_gameSelectorModel.getGameData() != null) {
        m_gameSelectorModel.getGameData().clearAllListeners();
      }
    } catch (final Exception e) {
    }
    s_instance = null;
    m_setupPanelModel = null;
    m_iGame = null;
    System.out.println("Shutdown Script Finished.");
  }

  public void waitForUsersHeadless() {
    setServerGame(null);

    final Runnable r = () -> {
      while (!m_shutDown) {
        if(!ThreadUtil.sleep(8000)) {
          m_shutDown = true;
          break;
        }
        if (m_setupPanelModel != null && m_setupPanelModel.getPanel() != null
            && m_setupPanelModel.getPanel().canGameStart()) {
          final boolean started = startHeadlessGame(m_setupPanelModel);
          if (!started) {
            System.out.println("Error in launcher, going back to waiting.");
          } else {
            // TODO: need a latch instead?
            break;
          }
        }
      }
    };
    final Thread t = new Thread(r, "Headless Server Waiting For Users To Connect And Start");
    t.start();
  }

  private static synchronized boolean startHeadlessGame(final SetupPanelModel setupPanelModel) {
    try {
      if (setupPanelModel != null && setupPanelModel.getPanel() != null && setupPanelModel.getPanel().canGameStart()) {
        System.out.println("Starting Game: " + setupPanelModel.getGameSelectorModel().getGameData().getGameName()
            + ", Round: " + setupPanelModel.getGameSelectorModel().getGameData().getSequence().getRound());
        setupPanelModel.getPanel().preStartGame();
        final ILauncher launcher = setupPanelModel.getPanel().getLauncher();
        if (launcher != null) {
          launcher.launch(null);
        }
        setupPanelModel.getPanel().postStartGame();
        return launcher != null;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      final ServerModel model = getServerModel(setupPanelModel);
      if (model != null) {
        // if we do not do this, we can get into an infinite loop of launching a game, then crashing out,
        // then launching, etc.
        model.setAllPlayersToNullNodes();
      }
    }
    return false;
  }

  public static void waitForUsersHeadlessInstance() {
    final HeadlessGameServer server = getInstance();
    if (server == null) {
      System.err.println("Couldn't find instance.");
      System.exit(-1);
    } else {
      System.out.println("Waiting for users to connect.");
      server.waitForUsersHeadless();
    }
  }

  SetupPanelModel getSetupPanelModel() {
    return m_setupPanelModel;
  }

  ServerModel getServerModel() {
    return getServerModel(m_setupPanelModel);
  }

  static ServerModel getServerModel(final SetupPanelModel setupPanelModel) {
    if (setupPanelModel == null) {
      return null;
    }
    final ISetupPanel setup = setupPanelModel.getPanel();
    if (setup == null) {
      return null;
    }
    if (setup instanceof ServerSetupPanel) {
      return ((ServerSetupPanel) setup).getModel();
    } else if (setup instanceof HeadlessServerSetup) {
      return ((HeadlessServerSetup) setup).getModel();
    }
    return null;
  }

  /**
   * todo, replace with something better
   * Get the chat for the game, or null if there is no chat
   */
  public Chat getChat() {
    final ISetupPanel model = m_setupPanelModel.getPanel();
    if (model instanceof ServerSetupPanel) {
      return model.getChatPanel().getChat();
    } else if (model instanceof ClientSetupPanel) {
      return model.getChatPanel().getChat();
    } else if (model instanceof HeadlessServerSetup) {
      return model.getChatPanel().getChat();
    } else {
      return null;
    }
  }

  public static void main(final String[] args) {
    GameRunner.handleCommandLineArgs(args, getProperties(), GameRunner.GameMode.HEADLESS_BOT);
    ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
    try {
      new HeadlessGameServer();
    } catch (final Exception e) {
      ClientLogger.logError("Failed to start game server: " + e);
    }
  }
}
