package org.triplea.game.server;

import com.google.common.base.Preconditions;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;

/** A way of hosting a game, but headless. */
@Slf4j
public class HeadlessGameServer {
  public static final String BOT_GAME_HOST_COMMENT = "automated_host";
  public static final String BOT_GAME_HOST_NAME_PREFIX = "Bot";
  private static HeadlessGameServer instance = null;

  private final InstalledMapsListing availableGames = InstalledMapsListing.parseMapFiles();
  private final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private final HeadlessServerSetupModel setupPanelModel =
      new HeadlessServerSetupModel(gameSelectorModel, this);
  private ServerGame game = null;
  private boolean shutDown = false;

  private HeadlessGameServer() {}

  public static void initializeHeadlessGameServerInstance() {
    Preconditions.checkState(headless(), "TripleA must be headless to invoke this method!");
    if (instance != null) {
      throw new IllegalStateException("Instance already exists");
    }
    instance = new HeadlessGameServer();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Running ShutdownHook.");
                  instance.shutDown = true;
                  Optional.ofNullable(instance.game).ifPresent(ServerGame::stopGame);
                  Optional.ofNullable(instance.setupPanelModel.getPanel())
                      .ifPresent(HeadlessServerSetup::cancel);
                }));

    ThreadRunner.runInNewThread(
        () -> {
          log.info("Headless Start");
          instance.setupPanelModel.showSelectType();
          instance.waitForUsersHeadless();
        });
  }

  /**
   * Returns the singleton instance of the HeadlessGameServer object.
   *
   * @deprecated Do not use this method, pass the instance along call trees to avoid static
   *     coupling.
   */
  @Deprecated
  public static synchronized HeadlessGameServer getInstance() {
    return instance;
  }

  public static synchronized boolean headless() {
    return Boolean.parseBoolean(System.getProperty(GameRunner.TRIPLEA_HEADLESS, "false"));
  }

  public Collection<String> getAvailableGames() {
    return availableGames.getSortedGameList();
  }

  public synchronized void setGameMapTo(final String gameName) {
    log.info("Requested to change map to: " + gameName);
    // don't change mid-game and only if we have the game
    if (setupPanelModel.getPanel() != null && game == null && availableGames.hasGame(gameName)) {
      gameSelectorModel.load(availableGames.findGameXmlPathByGameName(gameName).orElseThrow());
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

  public synchronized void loadGameSave(final Path file) {
    Preconditions.checkArgument(
        Files.exists(file), "File must exist to load it: " + file.toAbsolutePath());
    // don't change mid-game
    if (setupPanelModel.getPanel() != null && game == null && gameSelectorModel.load(file)) {
      log.info("Changed to save: " + file.getFileName());
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
      final GameState data = gameSelectorModel.getGameData();
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
  public synchronized void setServerGame(final ServerGame serverGame) {
    game = serverGame;
    if (serverGame != null) {
      log.info(
          "Game starting up: "
              + game.isGameSequenceRunning()
              + ", GameOver: "
              + game.isGameOver()
              + ", Players: "
              + game.getPlayerManager().toString());
    }
  }

  public void waitForUsersHeadless() {
    log.info("Waiting for users to connect.");
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
                  final boolean started = startHeadlessGame();
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

  private synchronized boolean startHeadlessGame() {
    try {
      if (setupPanelModel.getPanel() != null && setupPanelModel.getPanel().canGameStart()) {
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
                      ThreadRunner.runInNewThread(launcher::launch);
                      return true;
                    })
                .orElse(false);
        setupPanelModel.getPanel().postStartGame();
        return launched;
      }
    } catch (final Exception e) {
      log.error("Failed to start headless game", e);
      Optional.ofNullable(setupPanelModel.getPanel())
          .map(HeadlessServerSetup::getModel)
          .ifPresent(ServerModel::setAllPlayersToNullNodes);
    }
    return false;
  }

  /** todo, replace with something better Get the chat for the game, or null if there is no chat. */
  public Chat getChat() {
    final SetupModel model = setupPanelModel.getPanel();
    return model != null ? model.getChatModel().getChat() : null;
  }
}
