package games.strategy.engine.framework.headlessGameServer;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_RECONNECTION;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_SUPPORT_EMAIL;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_SUPPORT_PASSWORD;
import static games.strategy.engine.framework.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.CliProperties.LOBBY_PORT;
import static games.strategy.engine.framework.CliProperties.MAP_FOLDER;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import games.strategy.debug.DebugUtils;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.headless.game.server.ArgValidationResult;
import games.strategy.engine.framework.headless.game.server.HeadlessGameServerCliParam;
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
import games.strategy.util.ExitStatus;
import games.strategy.util.Interruptibles;
import games.strategy.util.Md5Crypt;
import games.strategy.util.TimeManager;
import games.strategy.util.Util;
import lombok.extern.java.Log;

/**
 * A way of hosting a game, but headless.
 */
@Log
public class HeadlessGameServer {
  private final AvailableGames availableGames;
  private final GameSelectorModel gameSelectorModel;
  private final ScheduledExecutorService lobbyWatcherResetupThread = Executors.newScheduledThreadPool(1);
  private final String startDate = TimeManager.getFullUtcString(Instant.now());
  private static HeadlessGameServer instance = null;
  private SetupPanelModel setupPanelModel = null;
  private ServerGame game = null;
  private boolean shutDown = false;

  private final List<Runnable> shutdownListeners = Arrays.asList(
      lobbyWatcherResetupThread::shutdown,
      () -> Optional.ofNullable(game).ifPresent(ServerGame::stopGame),
      () -> Optional.ofNullable(setupPanelModel)
          .ifPresent(model -> model.getPanel().cancel()));


  private HeadlessGameServer() {
    if (instance != null) {
      throw new IllegalStateException("Instance already exists");
    }
    instance = this;
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Running ShutdownHook.");
      shutDown = true;
      shutdownListeners.stream()
          .forEach(Runnable::run);
    }));
    availableGames = new AvailableGames();
    gameSelectorModel = new GameSelectorModel();
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (!fileName.isEmpty()) {
      try {
        final File file = new File(fileName);
        if (file.exists()) {
          gameSelectorModel.load(file);
        }
      } catch (final Exception e) {
        gameSelectorModel.resetGameDataToNull();
      }
    }
    new Thread(() -> {
      log.info("Headless Start");
      setupPanelModel = new HeadlessServerSetupPanelModel(gameSelectorModel);
      setupPanelModel.showSelectType();
      log.info("Waiting for users to connect.");
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
    log.info("Game Server initialized");
  }

  public static synchronized HeadlessGameServer getInstance() {
    return instance;
  }

  public static synchronized boolean headless() {
    return getInstance() != null || Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"));
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
      log.info("Changed to game map: " + gameName);
    }
  }

  public synchronized void loadGameSave(final File file) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (file == null || !file.exists()) {
        return;
      }
      gameSelectorModel.load(file);
      log.info("Changed to save: " + file.getName());
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
        log.info("Loading GameData failed for: " + fileName);
        return;
      }


      final String mapNameProperty = data.getProperties().get(Constants.MAP_NAME, "");


      if (!availableGames.containsMapName(mapNameProperty)) {
        log.info("Game mapName not in available games listing: " + mapNameProperty);
        return;
      }
      gameSelectorModel.load(data, fileName);
      log.info("Changed to user savegame: " + fileName);
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
      log.info("Changed to user game options.");
    }
  }

  public static synchronized void setServerGame(final ServerGame serverGame) {
    final HeadlessGameServer instance = getInstance();
    if (instance != null) {
      instance.game = serverGame;
      if (serverGame != null) {
        log.info("Game starting up: " + instance.game.isGameSequenceRunning() + ", GameOver: "
            + instance.game.isGameOver() + ", Players: " + instance.game.getPlayerManager().toString());
      }
    }
  }

  public static synchronized void log(final String stdout) {
    final HeadlessGameServer instance = getInstance();
    if (instance != null) {
      log.info(stdout);
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
          log.log(Level.SEVERE, "Failed to send chat", e);
        }
      }
    }
  }

  public String getSalt() {
    return Md5Crypt.newSalt();
  }

  private static String hashPassword(final String password, final String salt) {
    return Md5Crypt.hashPassword(password, salt);
  }

  public String remoteShutdown(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    if (hashPassword(password, salt).equals(hashedPassword)) {
      new Thread(() -> {
        log.info("Remote Shutdown Initiated.");
        ExitStatus.SUCCESS.exit();
      }).start();
      return null;
    }
    log.info("Attempted remote shutdown with invalid password.");
    return "Invalid password!";
  }

  public String remoteStopGame(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    if (hashPassword(password, salt).equals(hashedPassword)) {
      final ServerGame serverGame = game;
      if (serverGame != null) {
        new Thread(() -> {
          log.info("Remote Stop Game Initiated.");
          try {
            serverGame.saveGame(new File(
                ClientSetting.SAVE_GAMES_FOLDER_PATH.value(),
                SaveGameFileChooser.getAutoSaveFileName()));
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to save game", e);
          }
          serverGame.stopGame();
        }).start();
      }
      return null;
    }
    log.info("Attempted remote stop game with invalid password.");
    return "Invalid password!";
  }

  public String remoteGetChatLog(final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    if (hashPassword(password, salt).equals(hashedPassword)) {
      final IChatPanel chat = getServerModel().getChatPanel();
      if (chat == null || chat.getAllText() == null) {
        return "Empty or null chat";
      }
      return chat.getAllText();
    }
    log.info("Attempted remote get chat log with invalid password.");
    return "Invalid password!";
  }

  public String remoteMutePlayer(final String playerName, final int minutes, final String hashedPassword,
      final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    // (48 hours max)
    final Instant expire = Instant.now().plus(Duration.ofMinutes(Math.min(60 * 24 * 2, minutes)));
    if (hashPassword(password, salt).equals(hashedPassword)) {
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
              log.info("Remote Mute of Player: " + playerName);
              messenger.notifyUsernameMutingOfPlayer(realName, expire);
              messenger.notifyIpMutingOfPlayer(ip, expire);
              messenger.notifyMacMutingOfPlayer(mac, expire);
              return;
            }
          }
        } catch (final Exception e) {
          log.log(Level.SEVERE, "Failed to notify mute of player", e);
        }
      }).start();
      return null;
    }
    log.info("Attempted remote mute player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBootPlayer(final String playerName, final String hashedPassword, final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    if (hashPassword(password, salt).equals(hashedPassword)) {
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
              log.info("Remote Boot of Player: " + playerName);
              messenger.removeConnection(node);
            }
          }
        } catch (final Exception e) {
          log.log(Level.SEVERE, "Failed to notify boot of player", e);
        }
      }).start();
      return null;
    }
    log.warning("Attempted remote boot player with invalid password.");
    return "Invalid password!";
  }

  public String remoteBanPlayer(final String playerName, final int hours, final String hashedPassword,
      final String salt) {
    final String password = System.getProperty(LOBBY_GAME_SUPPORT_PASSWORD, "");
    if (password.equals(GameRunner.NO_REMOTE_REQUESTS_ALLOWED)) {
      return "Host not accepting remote requests!";
    }
    // milliseconds (30 days max)
    final Instant expire = Instant.now().plus(Duration.ofHours(Math.min(24 * 30, hours)));
    if (hashPassword(password, salt).equals(hashedPassword)) {
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
              log.info("Remote Ban of Player: " + playerName);
              try {
                messenger.notifyUsernameMiniBanningOfPlayer(realName, expire);
              } catch (final Exception e) {
                log.log(Level.SEVERE, "Failed to notify username ban of player", e);
              }
              try {
                messenger.notifyIpMiniBanningOfPlayer(ip, expire);
              } catch (final Exception e) {
                log.log(Level.SEVERE, "Failed to notify IP ban of player", e);
              }
              try {
                messenger.notifyMacMiniBanningOfPlayer(mac, expire);
              } catch (final Exception e) {
                log.log(Level.SEVERE, "Failed to notify MAC ban of player", e);
              }
              messenger.removeConnection(node);
            }
          }
        } catch (final Exception e) {
          log.log(Level.SEVERE, "Failed to notify ban of player", e);
        }
      }).start();
      return null;
    }
    log.warning("Attempted remote ban player with invalid password.");
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
      log.log(Level.SEVERE, "Failed to restart lobby watcher", e);
    }
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
    log.info("Dump to Log:"
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
            log.warning("Error in launcher, going back to waiting.");
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
        log.info("Starting Game: " + setupPanelModel.getGameSelectorModel().getGameData().getGameName()
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
      log.log(Level.SEVERE, "Failed to start headless game", e);
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
    log.info("Waiting for users to connect.");
    instance.waitForUsersHeadless();
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
    final ArgValidationResult validation = HeadlessGameServerCliParam.validateArgs(args);
    if (!validation.isValid()) {
      log.log(Level.SEVERE,
          String.format("Failed to start, improper args: %s\n"
                  + "Errors:\n- %s\n"
                  + "Example usage: %s",
              Arrays.toString(args),
              String.join("\n- ", validation.getErrorMessages()),
              HeadlessGameServerCliParam.exampleUsage()));
      return;
    }

    ClientSetting.initialize();
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    ArgParser.handleCommandLineArgs(args);
    handleHeadlessGameServerArgs();
    ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
    try {
      new HeadlessGameServer();
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to start game server", e);
    }
  }

  private static void usage() {
    // TODO replace this method with the generated usage of commons-cli
    log.info("\nUsage and Valid Arguments:\n"
        + "   " + TRIPLEA_GAME + "=<FILE_NAME>\n"
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

    final File mapFolder = new File(ClientSetting.MAP_FOLDER_OVERRIDE.value());
    if (!mapFolder.isDirectory()) {
      log.warning("Invalid '" + MAP_FOLDER + "' param, map folder must exist: '" + mapFolder + "'");
      printUsage = true;
    }

    final String playerName = System.getProperty(TRIPLEA_NAME, "");
    final String hostName = System.getProperty(LOBBY_GAME_HOSTED_BY, "");
    if (playerName.length() < 7 || hostName.length() < 7 || !hostName.equals(playerName)
        || !playerName.startsWith("Bot") || !hostName.startsWith("Bot")) {
      log.warning(
          "Invalid argument: " + TRIPLEA_NAME + " and " + LOBBY_GAME_HOSTED_BY
              + " must start with \"Bot\" and be at least 7 characters long and be the same.");
      printUsage = true;
    }

    final String comments = System.getProperty(LOBBY_GAME_COMMENTS, "");
    if (!comments.contains("automated_host")) {
      log.warning(
          "Invalid argument: " + LOBBY_GAME_COMMENTS + " must contain the string \"automated_host\".");
      printUsage = true;
    }

    final String email = System.getProperty(LOBBY_GAME_SUPPORT_EMAIL, "");
    if (email.length() < 3 || !Util.isMailValid(email)) {
      log.warning(
          "Invalid argument: " + LOBBY_GAME_SUPPORT_EMAIL + " must contain a valid email address.");
      printUsage = true;
    }

    final String reconnection = System.getProperty(LOBBY_GAME_RECONNECTION,
        "" + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
    try {
      final int reconnect = Integer.parseInt(reconnection);
      if (reconnect < GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM) {
        log.warning("Invalid argument: " + LOBBY_GAME_RECONNECTION
            + " must be an integer equal to or greater than " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
            + " seconds, and should normally be either " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT
            + " or " + (2 * GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
        printUsage = true;
      }
    } catch (final NumberFormatException e) {
      log.warning("Invalid argument: " + LOBBY_GAME_RECONNECTION
          + " must be an integer equal to or greater than " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
          + " seconds, and should normally be either " + GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT + " or "
          + (2 * GameRunner.LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
      printUsage = true;
    }

    if (printUsage) {
      usage();
      ExitStatus.FAILURE.exit();
    }
  }
}
