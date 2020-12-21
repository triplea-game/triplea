package org.triplea.game.server;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.MAP_FOLDER;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.map.file.system.loader.AvailableGamesFileSystemReader;
import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.Interruptibles;
import org.triplea.util.ExitStatus;

/** A way of hosting a game, but headless. */
@Slf4j
public class HeadlessGameServer {
  public static final String BOT_GAME_HOST_COMMENT = "automated_host";
  public static final String BOT_GAME_HOST_NAME_PREFIX = "Bot";
  private static HeadlessGameServer instance = null;

  private final AvailableGamesList availableGames = AvailableGamesFileSystemReader.parseMapFiles();
  private final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private final HeadlessServerSetupPanelModel setupPanelModel =
      new HeadlessServerSetupPanelModel(gameSelectorModel);
  private ServerGame game = null;
  private boolean shutDown = false;

  private HeadlessGameServer() {
    if (instance != null) {
      throw new IllegalStateException("Instance already exists");
    }
    instance = this;

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Running ShutdownHook.");
                  shutDown = true;
                  Optional.ofNullable(game).ifPresent(ServerGame::stopGame);
                  Optional.ofNullable(setupPanelModel.getPanel())
                      .ifPresent(HeadlessServerSetup::cancel);
                }));

    new Thread(
            () -> {
              log.info("Headless Start");
              setupPanelModel.showSelectType();
              log.info("Waiting for users to connect.");
              waitForUsersHeadless();
            },
            "Initialize Headless Server Setup Model")
        .start();

    log.info("Game Server initialized");
  }

  public static synchronized HeadlessGameServer getInstance() {
    return instance;
  }

  public static synchronized boolean headless() {
    return instance != null
        || Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"));
  }

  public Collection<String> getAvailableGames() {
    return availableGames.getSortedGameList();
  }

  public synchronized void setGameMapTo(final String gameName) {
    log.info("Requested to change map to: " + gameName);
    // don't change mid-game and only if we have the game
    if (setupPanelModel.getPanel() != null && game == null && availableGames.hasGame(gameName)) {
      gameSelectorModel.load(availableGames.findGameUriByName(gameName).orElseThrow());
      log.info("Changed to game map: " + gameName);
    } else {
      log.info(
          String.format(
              "Did NOT change game map to: %s, "
                  + "getPanel == null ? %s, "
                  + "game == null ? %s, "
                  + "have game? %s",
              gameName,
              setupPanelModel.getPanel() != null,
              game == null,
              availableGames.hasGame(gameName)));
    }
  }

  public synchronized void loadGameSave(final File file) {
    Preconditions.checkArgument(
        file.exists(), "File must exist to load it: " + file.getAbsolutePath());
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null && gameSelectorModel.load(file)) {
      log.info("Changed to save: " + file.getName());
    }
  }

  /**
   * Loads a save game from the specified stream.
   *
   * @param input The stream containing the save game.
   */
  public synchronized void loadGameSave(final InputStream input) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      GameDataManager.loadGame(input)
          .filter(this::checkGameIsAvailableOnServer)
          .ifPresent(gameSelectorModel::setGameData);
    }
  }

  private boolean checkGameIsAvailableOnServer(final GameData gameData) {
    if (availableGames.hasGame(gameData.getGameName())) {
      return true;
    } else {
      log.warn("Game is not installed on this server: " + gameData.getGameName());
      return false;
    }
  }

  /**
   * Loads the game properties from the specified byte array and applies them to the
   * currently-selected game.
   *
   * @param bytes The serialized game properties.
   */
  public synchronized void loadGameOptions(final byte[] bytes) {
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null) {
      if (bytes == null || bytes.length == 0) {
        return;
      }
      final GameDataInjections data = gameSelectorModel.getGameData();
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

  /** Updates current 'HeadlessGameServer.game' instance to be set to the given parameter. */
  public static synchronized void setServerGame(final ServerGame serverGame) {
    if (instance != null) {
      instance.game = serverGame;
      if (serverGame != null) {
        log.info(
            "Game starting up: "
                + instance.game.isGameSequenceRunning()
                + ", GameOver: "
                + instance.game.isGameOver()
                + ", Players: "
                + instance.game.getPlayerManager().toString());
      }
    }
  }

  private void waitForUsersHeadless() {
    setServerGame(null);

    new Thread(
            () -> {
              while (!shutDown) {
                if (!Interruptibles.sleep(8000)) {
                  shutDown = true;
                  break;
                }
                if (setupPanelModel.getPanel() != null
                    && setupPanelModel.getPanel().canGameStart()) {
                  final boolean started = startHeadlessGame(setupPanelModel, gameSelectorModel);
                  if (!started) {
                    log.warn("Error in launcher, going back to waiting.");
                  } else {
                    // TODO: need a latch instead?
                    break;
                  }
                }
              }
            },
            "Headless Server Waiting For Users To Connect And Start")
        .start();
  }

  private static synchronized boolean startHeadlessGame(
      final HeadlessServerSetupPanelModel setupPanelModel,
      final GameSelectorModel gameSelectorModel) {
    try {
      if (setupPanelModel != null
          && setupPanelModel.getPanel() != null
          && setupPanelModel.getPanel().canGameStart()) {
        log.info(
            "Starting Game: "
                + gameSelectorModel.getGameData().getGameName()
                + ", Round: "
                + gameSelectorModel.getGameData().getSequence().getRound());

        final boolean launched =
            setupPanelModel
                .getPanel()
                .getLauncher()
                .map(
                    launcher -> {
                      new Thread(launcher::launch).start();
                      return true;
                    })
                .orElse(false);
        setupPanelModel.getPanel().postStartGame();
        return launched;
      }
    } catch (final Exception e) {
      log.error("Failed to start headless game", e);
      final ServerModel model = getServerModel(setupPanelModel);
      if (model != null) {
        // if we do not do this, we can get into an infinite loop of launching a game, then crashing
        // out,
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

  private static ServerModel getServerModel(final HeadlessServerSetupPanelModel setupPanelModel) {
    return Optional.ofNullable(setupPanelModel)
        .map(HeadlessServerSetupPanelModel::getPanel)
        .map(HeadlessServerSetup::getModel)
        .orElse(null);
  }

  /** todo, replace with something better Get the chat for the game, or null if there is no chat. */
  public Chat getChat() {
    final SetupModel model = setupPanelModel.getPanel();
    return model != null ? model.getChatModel().getChat() : null;
  }

  /**
   * Starts a new headless game server. This method will return before the headless game server
   * exits. The headless game server runs until the process is killed or the headless game server is
   * shut down via administrative command.
   *
   * <p>Most properties are passed via command line-like arguments.
   */
  public static void start(final String[] args) {
    ClientSetting.initialize();
    System.setProperty(LOBBY_GAME_COMMENTS, BOT_GAME_HOST_COMMENT);
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    System.setProperty(TRIPLEA_SERVER, "true");

    ArgParser.handleCommandLineArgs(args);
    handleHeadlessGameServerArgs();
    try {
      new HeadlessGameServer();
    } catch (final Exception e) {
      log.error("Failed to start game server", e);
    }
  }

  private static void usage() {
    // TODO replace this method with the generated usage of commons-cli
    log.info(
        "\nUsage and Valid Arguments:\n"
            + "   "
            + TRIPLEA_GAME
            + "=<FILE_NAME>\n"
            + "   "
            + TRIPLEA_PORT
            + "=<PORT>\n"
            + "   "
            + TRIPLEA_NAME
            + "=<PLAYER_NAME>\n"
            + "   "
            + LOBBY_URI
            + "=<LOBBY_URI>\n"
            + "   "
            + MAP_FOLDER
            + "=<MAP_FOLDER>"
            + "\n");
  }

  private static void handleHeadlessGameServerArgs() {
    boolean printUsage = false;

    if (!ClientSetting.mapFolderOverride.isSet()) {
      ClientSetting.mapFolderOverride.setValue(ClientFileSystemHelper.getUserMapsFolder().toPath());
    }

    final String playerName = System.getProperty(TRIPLEA_NAME, "");
    if ((playerName.length() < 7) || !playerName.startsWith(BOT_GAME_HOST_NAME_PREFIX)) {
      log.warn(
          "Invalid or missing argument: "
              + TRIPLEA_NAME
              + " must at least 7 characters long "
              + "and start with "
              + BOT_GAME_HOST_NAME_PREFIX);
      printUsage = true;
    }

    if (isInvalidPortNumber(System.getProperty(TRIPLEA_PORT, "0"))) {
      log.warn("Invalid or missing argument: " + TRIPLEA_PORT + " must be greater than zero");
      printUsage = true;
    }

    if (System.getProperty(LOBBY_URI, "").isEmpty()) {
      log.warn("Invalid or missing argument: " + LOBBY_URI + " must be set");
      printUsage = true;
    }

    if (printUsage) {
      usage();
      ExitStatus.FAILURE.exit();
    }
  }

  private static boolean isInvalidPortNumber(final String testValue) {
    try {
      return Integer.parseInt(testValue) <= 0;
    } catch (final NumberFormatException e) {
      return true;
    }
  }
}
