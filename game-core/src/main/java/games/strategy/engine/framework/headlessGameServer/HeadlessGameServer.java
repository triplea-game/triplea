package games.strategy.engine.framework.headlessGameServer;

import static games.strategy.engine.framework.ArgParser.CliProperties.GAME_HOST_CONSOLE;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_RECONNECTION;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_SUPPORT_EMAIL;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_SUPPORT_PASSWORD;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.ArgParser.CliProperties.MAP_FOLDER;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.debug.DebugUtils;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.Constants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Interruptibles;
import games.strategy.util.TimeManager;
import games.strategy.util.Util;

/**
 * A way of hosting a game, but headless.
 */
public class HeadlessGameServer {

  private static final Logger logger = Logger.getLogger(HeadlessGameServer.class.getName());
  private final AvailableGames availableGames;
  private final GameSelectorModel gameSelectorModel;
  private final ScheduledExecutorService lobbyWatcherResetupThread = Executors.newScheduledThreadPool(1);
  private final String startDate = TimeManager.getFullUtcString(Instant.now());
  private static HeadlessGameServer instance = null;
  private SetupPanelModel setupPanelModel = null;
  private ServerGame game = null;
  private boolean shutDown = false;

  private HeadlessGameServer() {
    if (instance != null) {
      throw new IllegalStateException("Instance already exists");
    }
    instance = this;
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Running ShutdownHook.");
      shutdown();
    }));
    availableGames = new AvailableGames();
    gameSelectorModel = new GameSelectorModel();
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.length() > 0) {
      try {
        final File file = new File(fileName);
        gameSelectorModel.load(file, null);
      } catch (final Exception e) {
        gameSelectorModel.resetGameDataToNull();
      }
    }
    new Thread(() -> {
      System.out.println("Headless Start");
      setupPanelModel = new HeadlessServerSetupPanelModel(gameSelectorModel, null);
      setupPanelModel.showSelectType();
      System.out.println("Waiting for users to connect.");
      waitForUsersHeadless();
    }, "Initialize Headless Server Setup Model").start();

    int reconnect;
    try {
      final String reconnectionSeconds = System.getProperty(LOBBY_GAME_RECONNECTION,
          "" + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
      reconnect =
          Math.max(Integer.parseInt(reconnectionSeconds), GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM);
    } catch (final NumberFormatException e) {
      reconnect = GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT;
    }
    lobbyWatcherResetupThread.scheduleAtFixedRate(() -> {
      try {
        restartLobbyWatcher(setupPanelModel, game);
      } catch (final Exception e) {
        Interruptibles.sleep(10 * 60 * 1000);
        // try again, but don't catch it this time
        restartLobbyWatcher(setupPanelModel, game);
      }
    }, reconnect, reconnect, TimeUnit.SECONDS);
    logger.info("Game Server initialized");
  }

  public static synchronized HeadlessGameServer getInstance() {
    return instance;
  }

  public static synchronized boolean headless() {
    if (getInstance() != null) {
      return true;
    }
    return Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"));
  }

  public Set<String> getAvailableGames() {
    return availableGames.getGameNames();
  }

  public synchronized void setGameMapTo(final String gameName) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (!availableGames.getGameNames().contains(gameName)) {
        return;
      }
      gameSelectorModel.load(availableGames.getGameData(gameName), availableGames.getGameFilePath(gameName));
      System.out.println("Changed to game map: " + gameName);
    }
  }

  public synchronized void loadGameSave(final File file) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (file == null || !file.exists()) {
        return;
      }
      gameSelectorModel.load(file, null);
      System.out.println("Changed to save: " + file.getName());
    }
  }

  public synchronized void loadGameSave(final InputStream input, final String fileName) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (input == null || fileName == null) {
        return;
      }
      final GameData data = gameSelectorModel.getGameData(input);
      if (data == null) {
        System.out.println("Loading GameData failed for: " + fileName);
        return;
      }


      final String mapNameProperty = data.getProperties().get(Constants.MAP_NAME, "");


      if (!availableGames.containsMapName(mapNameProperty)) {
        System.out.println("Game mapName not in available games listing: " + mapNameProperty);
        return;
      }
      gameSelectorModel.load(data, fileName);
      System.out.println("Changed to user savegame: " + fileName);
    }
  }

  public synchronized void loadGameOptions(final byte[] bytes) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (bytes == null || bytes.length == 0) {
        return;
      }
      final GameData data = gameSelectorModel.getGameData();
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
      instance.game = serverGame;
      if (serverGame != null) {
        System.out.println("Game starting up: " + instance.game.isGameSequenceRunning() + ", GameOver: "
            + instance.game.isGameOver() + ", Players: " + instance.game.getPlayerManager().toString());
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
          logger.log(Level.SEVERE, "Failed to send chat", e);
        }
      }
    }
  }

  public String getSalt() {
    final String encryptedPassword = md5Crypt(System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, ""));
    return games.strategy.util.Md5Crypt.getSalt(encryptedPassword);
  }

  private static String md5Crypt(final String value) {
    return games.strategy.util.Md5Crypt.crypt(value);
  }

  private static String md5Crypt(final String value, final String salt) {
    return games.strategy.util.Md5Crypt.crypt(value, salt);
  }

  public String remoteShutdown(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      new Thread(() -> {
        System.out.println("Remote Shutdown Initiated.");
        System.exit(0);
      }).start();
      return null;
    }
    System.out.println("Attempted remote shutdown with invalid password.");
    return "Invalid password!";
  }

  public String remoteStopGame(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      final ServerGame serverGame = game;
      if (serverGame != null) {
        new Thread(() -> {
          System.out.println("Remote Stop Game Initiated.");
          try {
            serverGame.saveGame(new File(
                ClientSetting.SAVE_GAMES_FOLDER_PATH.value(),
                SaveGameFileChooser.getAutoSaveFileName()));
          } catch (final Exception e) {
            logger.log(Level.SEVERE, "Failed to save game", e);
          }
          serverGame.stopGame();
        }).start();
      }
      return null;
    }
    System.out.println("Attempted remote stop game with invalid password.");
    return "Invalid password!";
  }

  public String remoteGetChatLog(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
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
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
    // (48 hours max)
    final Instant expire = Instant.now().plus(Duration.ofMinutes(Math.min(60 * 24 * 2, minutes)));
    if (encryptedPassword.equals(hashedPassword)) {
      new Thread(() -> {
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
              messenger.notifyUsernameMutingOfPlayer(realName, expire);
              messenger.notifyIpMutingOfPlayer(ip, expire);
              messenger.notifyMacMutingOfPlayer(mac, expire);
              return;
            }
          }
        } catch (final Exception e) {
          logger.log(Level.SEVERE, "Failed to notify mute of player", e);
        }
      }).start();
      return null;
    }
    System.out.println("Attempted remote mute player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBootPlayer(final String playerName, final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
    if (encryptedPassword.equals(hashedPassword)) {
      new Thread(() -> {
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
          logger.log(Level.SEVERE, "Failed to notify boot of player", e);
        }
      }).start();
      return null;
    }
    System.out.println("Attempted remote boot player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBanPlayer(final String playerName, final int hours, final String hashedPassword,
      final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    final String localPassword = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    final String encryptedPassword = md5Crypt(localPassword, salt);
    // milliseconds (30 days max)
    final Instant expire = Instant.now().plus(Duration.ofHours(Math.min(24 * 30, hours)));
    if (encryptedPassword.equals(hashedPassword)) {
      new Thread(() -> {
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
                messenger.notifyUsernameMiniBanningOfPlayer(realName, expire);
              } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to notify username ban of player", e);
              }
              try {
                messenger.notifyIpMiniBanningOfPlayer(ip, expire);
              } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to notify IP ban of player", e);
              }
              try {
                messenger.notifyMacMiniBanningOfPlayer(mac, expire);
              } catch (final Exception e) {
                logger.log(Level.SEVERE, "Failed to notify MAC ban of player", e);
              }
              messenger.removeConnection(node);
            }
          }
        } catch (final Exception e) {
          logger.log(Level.SEVERE, "Failed to notify ban of player", e);
        }
      }).start();
      return null;
    }
    System.out.println("Attempted remote ban player with invalid password.");
    return "Invalid password!";
  }

  ServerGame getIGame() {
    return game;
  }

  private static synchronized void restartLobbyWatcher(
      final SetupPanelModel setupPanelModel, final ServerGame serverGame) {
    try {
      final ISetupPanel setup = setupPanelModel.getPanel();
      if (setup == null) {
        return;
      }
      if (serverGame != null) {
        return;
      }
      if (setup.canGameStart()) {
        return;
      }
      if (setup instanceof ServerSetupPanel) {
        ((ServerSetupPanel) setup).repostLobbyWatcher(serverGame);
      } else if (setup instanceof HeadlessServerSetup) {
        ((HeadlessServerSetup) setup).repostLobbyWatcher(serverGame);
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to restart lobby watcher", e);
    }
  }

  public static void resetLobbyHostOldExtensionProperties() {
    for (final String property : getProperties()) {
      if (LOBBY_HOST.equals(property)
          || LOBBY_PORT.equals(property)
          || LOBBY_GAME_HOSTED_BY.equals(property)) {
        // for these 3 properties, we clear them after hosting, but back them up.
        final String oldValue = System.getProperty(property + GameRunner.OLD_EXTENSION);
        if (oldValue != null) {
          System.setProperty(property, oldValue);
        }
      }
    }
  }

  private static Set<String> getProperties() {
    return new HashSet<>(Arrays.asList(TRIPLEA_GAME, GAME_HOST_CONSOLE,
        TRIPLEA_SERVER, TRIPLEA_PORT,
        TRIPLEA_NAME, LOBBY_HOST, LOBBY_PORT,
        LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY, LOBBY_GAME_SUPPORT_EMAIL,
        LOBBY_GAME_SUPPORT_PASSWORD, LOBBY_GAME_RECONNECTION,
        TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME,
        MAP_FOLDER));
  }

  String getStatus() {
    String message = "Server Start Date: " + startDate;
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
    System.out.println("Dump to Log:"
        + "\n\nStatus:\n"
        + getStatus()
        + "\n\nServer:\n"
        + getServerModel()
        + "\n\n"
        + DebugUtils.getThreadDumps()
        + "\n\n"
        + DebugUtils.getMemory()
        + "\n\nDump finished.\n");
  }

  synchronized void shutdown() {
    shutDown = true;
    try {
      if (lobbyWatcherResetupThread != null) {
        lobbyWatcherResetupThread.shutdown();
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to shutdown lobby watcher resetup thread", e);
    }
    try {
      if (game != null) {
        game.stopGame();
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to stop game", e);
    }
    try {
      if (setupPanelModel != null) {
        final ISetupPanel setup = setupPanelModel.getPanel();
        if (setup != null && setup instanceof HeadlessServerSetup) {
          setup.shutDown();
        }
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to shutdown setup panel", e);
    }
    try {
      if (gameSelectorModel != null && gameSelectorModel.getGameData() != null) {
        gameSelectorModel.getGameData().clearAllListeners();
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to clear all game data listeners", e);
    }
    instance = null;
    setupPanelModel = null;
    game = null;
    System.out.println("Shutdown Script Finished.");
  }

  private void waitForUsersHeadless() {
    setServerGame(null);

    new Thread(() -> {
      while (!shutDown) {
        if (!Interruptibles.sleep(8000)) {
          shutDown = true;
          break;
        }
        if (setupPanelModel != null && setupPanelModel.getPanel() != null
            && setupPanelModel.getPanel().canGameStart()) {
          final boolean started = startHeadlessGame(setupPanelModel);
          if (!started) {
            System.out.println("Error in launcher, going back to waiting.");
          } else {
            // TODO: need a latch instead?
            break;
          }
        }
      }
    }, "Headless Server Waiting For Users To Connect And Start").start();
  }

  private static synchronized boolean startHeadlessGame(final SetupPanelModel setupPanelModel) {
    try {
      if (setupPanelModel != null && setupPanelModel.getPanel() != null && setupPanelModel.getPanel().canGameStart()) {
        System.out.println("Starting Game: " + setupPanelModel.getGameSelectorModel().getGameData().getGameName()
            + ", Round: " + setupPanelModel.getGameSelectorModel().getGameData().getSequence().getRound());
        setupPanelModel.getPanel().preStartGame();

        final boolean launched = setupPanelModel.getPanel().getLauncher()
            .map(launcher -> {
              launcher.launch(null);
              return true;
            }).orElse(false);
        setupPanelModel.getPanel().postStartGame();
        return launched;
      }
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to start headless game", e);
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

  private ServerModel getServerModel() {
    return getServerModel(setupPanelModel);
  }

  private static ServerModel getServerModel(final SetupPanelModel setupPanelModel) {
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
   * Get the chat for the game, or null if there is no chat.
   */
  public Chat getChat() {
    final ISetupPanel model = setupPanelModel.getPanel();
    return ((model instanceof ServerSetupPanel)
        || (model instanceof ClientSetupPanel)
        || (model instanceof HeadlessServerSetup))
            ? model.getChatPanel().getChat()
            : null;
  }

  /**
   * Launches a bot server. Most properties are passed via command line-like arguments.
   */
  public static void main(final String[] args) {
    ClientSetting.initialize();

    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    // TODO: get properties from a configuration file instead of CLI.
    if (!new ArgParser(getProperties()).handleCommandLineArgs(args)) {
      usage();
      return;
    }

    handleHeadlessGameServerArgs();
    ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
    try {
      new HeadlessGameServer();
    } catch (final Exception e) {
      logger.log(Level.SEVERE, "Failed to start game server", e);
    }
  }

  private static void usage() {
    // TODO replace this method with the generated usage of commons-cli
    System.out.println("\nUsage and Valid Arguments:\n"
        + "   " + TRIPLEA_GAME + "=<FILE_NAME>\n"
        + "   " + GAME_HOST_CONSOLE + "=<true/false>\n"
        + "   " + TRIPLEA_SERVER + "=true\n"
        + "   " + TRIPLEA_PORT + "=<PORT>\n"
        + "   " + TRIPLEA_NAME + "=<PLAYER_NAME>\n"
        + "   " + LOBBY_HOST + "=<LOBBY_HOST>\n"
        + "   " + LOBBY_PORT + "=<LOBBY_PORT>\n"
        + "   " + LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n"
        + "   " + LOBBY_GAME_HOSTED_BY + "=<LOBBY_GAME_HOSTED_BY>\n"
        + "   " + LOBBY_GAME_SUPPORT_EMAIL + "=<youremail@emailprovider.com>\n"
        + "   " + LOBBY_GAME_SUPPORT_PASSWORD + "=<password for remote actions, such as remote stop game>\n"
        + "   " + LOBBY_GAME_RECONNECTION + "=<seconds between refreshing lobby connection [min "
        + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM + "]>\n"
        + "   " + TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME
        + "=<seconds to wait for all clients to start the game>\n"
        + "   " + TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME
        + "=<seconds to wait for an observer joining the game>\n"
        + "   " + MAP_FOLDER + "=mapFolder"
        + "\n"
        + "   You must start the Name and HostedBy with \"Bot\".\n"
        + "   Game Comments must have this string in it: \"automated_host\".\n"
        + "   You must include a support email for your host, so that you can be alerted by lobby admins when your "
        + "host has an error."
        + " (For example they may email you when your host is down and needs to be restarted.)\n"
        + "   Support password is a remote access password that will allow lobby admins to remotely take the "
        + "following actions: ban player, stop game, shutdown server."
        + " (Please email this password to one of the lobby moderators, or private message an admin on the "
        + "TripleaWarClub.org website forum.)\n");
  }

  private static void handleHeadlessGameServerArgs() {
    boolean printUsage = false;
    final String playerName = System.getProperty(TRIPLEA_NAME, "");
    final String hostName = System.getProperty(LOBBY_GAME_HOSTED_BY, "");
    if (playerName.length() < 7 || hostName.length() < 7 || !hostName.equals(playerName)
        || !playerName.startsWith("Bot") || !hostName.startsWith("Bot")) {
      System.out.println(
          "Invalid argument: " + TRIPLEA_NAME + " and " + LOBBY_GAME_HOSTED_BY
              + " must start with \"Bot\" and be at least 7 characters long and be the same.");
      printUsage = true;
    }

    final String comments = System.getProperty(LOBBY_GAME_COMMENTS, "");
    if (!comments.contains("automated_host")) {
      System.out.println(
          "Invalid argument: " + LOBBY_GAME_COMMENTS + " must contain the string \"automated_host\".");
      printUsage = true;
    }

    final String email = System.getProperty(LOBBY_GAME_SUPPORT_EMAIL, "");
    if (email.length() < 3 || !Util.isMailValid(email)) {
      System.out.println(
          "Invalid argument: " + LOBBY_GAME_SUPPORT_EMAIL + " must contain a valid email address.");
      printUsage = true;
    }

    final String reconnection = System.getProperty(LOBBY_GAME_RECONNECTION,
        "" + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
    try {
      final int reconnect = Integer.parseInt(reconnection);
      if (reconnect < GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM) {
        System.out.println("Invalid argument: " + LOBBY_GAME_RECONNECTION
            + " must be an integer equal to or greater than " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
            + " seconds, and should normally be either " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT
            + " or " + (2 * GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
        printUsage = true;
      }
    } catch (final NumberFormatException e) {
      System.out.println("Invalid argument: " + LOBBY_GAME_RECONNECTION
          + " must be an integer equal to or greater than " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
          + " seconds, and should normally be either " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT + " or "
          + (2 * GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
      printUsage = true;
    }

    if (printUsage) {
      usage();
      System.exit(-1);
    }
  }
}
