package org.triplea.game.server;

import com.google.common.base.Preconditions;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.HeadlessAutoSaveFileUtils;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;

/** A way of hosting a game, but headless. */
@Slf4j
public class HeadlessGameServer {
  private final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  @Nonnull private final HeadlessServerSetup headlessServerSetup;
  @Nullable private ServerGame game = null;
  @Nullable private Chat chat = null;

  private HeadlessGameServer() {
    headlessServerSetup =
        new HeadlessServerSetupModel(gameSelectorModel, this).createHeadlessServerSetup();
  }

  public static void runHeadlessGameServer() {
    Preconditions.checkState(
        GameRunner.headless(), "TripleA must be headless to invoke this method!");
    log.info("Headless Start");
    new HeadlessGameServer().start();
  }

  private void start() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Running ShutdownHook.");
                  Optional.ofNullable(game)
                      .ifPresent(
                          serverGame -> {
                            saveOnShutdown(serverGame);
                            notifyPlayersOfRestart();
                            serverGame.stopGame();
                          });
                  headlessServerSetup.cancel();
                }));

    waitForUsers();
  }

  /**
   * Saves the in-progress game to the headless auto-save slot so a restarted bot re-loads it (see
   * {@link GameSelectorModel#loadDefaultGameSameThread()}). Only the graceful SIGTERM shutdown path
   * reaches this; a hard kill (SIGKILL, cgroup OOM) bypasses it and falls back to the last
   * per-step/round auto-save.
   */
  private static void saveOnShutdown(final ServerGame serverGame) {
    final Path autoSaveFile = new HeadlessAutoSaveFileUtils().getHeadlessAutoSaveFile();
    try {
      serverGame.saveGame(autoSaveFile);
      log.info("Saved in-progress game on shutdown to {}", autoSaveFile);
    } catch (final RuntimeException e) {
      log.error("Failed to save in-progress game on shutdown to {}", autoSaveFile, e);
    }
  }

  /** Retains the current game's chat so the shutdown hook can announce a restart to players. */
  public void setChat(final Chat chat) {
    this.chat = chat;
  }

  /**
   * Tells connected players the bot is restarting and names the auto-save file it just wrote. The
   * name matches the client's "Loaded Savegame:" field exactly (both derive from {@link
   * HeadlessAutoSaveFileUtils#getHeadlessAutoSaveFile()}), so a player re-hosting from that save
   * can confirm it is the right one. The brief pause lets the message reach the relay before {@link
   * ServerGame#stopGame()} tears the connection down.
   */
  private void notifyPlayersOfRestart() {
    if (chat == null) {
      return;
    }
    final String autoSaveFileName =
        new HeadlessAutoSaveFileUtils().getHeadlessAutoSaveFile().getFileName().toString();
    chat.sendMessage("Bot is restarting. Auto-save file: " + autoSaveFileName);
    Interruptibles.sleep(2000);
  }

  public Collection<String> getAvailableGames() {
    return InstalledMapsListing.parseMapFiles().getSortedGameList();
  }

  public synchronized void setGameMapTo(final String gameName) {
    log.info("Requested to change map to: {}", gameName);

    // don't change mid-game and only if we have the game
    if (game != null) {
      log.info("Did NOT change game map to: {}, a game is currently running.", gameName);
      return;
    }

    final InstalledMapsListing availableGames = InstalledMapsListing.parseMapFiles();
    if (availableGames.hasGame(gameName)) {
      // change map
      gameSelectorModel.loadMap(availableGames.findGameXmlPathByGameName(gameName).orElseThrow());
      log.info("Changed to game map: {}", gameName);
    } else if (AutoSaveFileUtils.getAutoSaveFiles().contains(gameName)) {
      // change to autosave
      log.info("Loading {} as a save game", gameName);
      AutoSaveFileUtils.getAutoSavePaths().stream()
          .filter(p -> p.toFile().getName().equals(gameName))
          .findAny()
          .ifPresentOrElse(
              gameSelectorModel::loadSave,
              () ->
                  log.warn(
                      "Unexpected, could not find save file: {}, from choices: {}",
                      gameName,
                      AutoSaveFileUtils.getAutoSavePaths()));
    } else {
      log.warn("Unable to find save game as either a new map or a save game: {}", gameName);
    }
  }

  public synchronized void loadGameSave(final Path file) {
    Preconditions.checkArgument(
        Files.exists(file), "File must exist to load it: " + file.toAbsolutePath());
    // don't change mid-game
    if (game == null && gameSelectorModel.loadSave(file)) {
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
    if (game == null) {
      GameDataManager.loadGame(input)
          .filter(this::checkGameIsAvailableOnServer)
          .ifPresent(gameSelectorModel::setGameData);
    }
  }

  private boolean checkGameIsAvailableOnServer(final GameData gameData) {
    if (InstalledMapsListing.parseMapFiles().hasGame(gameData.getGameName())) {
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
    if (game == null) {
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
    if (game != null) {
      log.info(
          "Game starting up: "
              + game.isGameSequenceRunning()
              + ", GameOver: "
              + game.isGameOver()
              + ", Players: "
              + game.getPlayerManager().toString());
    }
  }

  public void waitForUsers() {
    log.info("Waiting for users to connect.");
    setServerGame(null);
    gameSelectorModel.loadDefaultGameSameThread();

    while (headlessServerSetup.waitUntilStart() && !startHeadlessGame()) {
      log.warn("Error in launcher, going back to waiting.");
    }
  }

  private boolean startHeadlessGame() {
    try {
      log.info(
          "Starting Game: "
              + gameSelectorModel.getGameData().getGameName()
              + ", Round: "
              + gameSelectorModel.getGameData().getSequence().getRound());

      final boolean launched =
          headlessServerSetup
              .getLauncher()
              .map(
                  launcher -> {
                    ThreadRunner.runInNewThread(launcher::launch);
                    return true;
                  })
              .orElse(false);
      headlessServerSetup.postStartGame();
      return launched;
    } catch (final Exception e) {
      log.error("Failed to start headless game", e);
      // if we do not do this, we can get into an infinite loop of launching a game, then crashing
      // out, then launching, etc.
      headlessServerSetup.getModel().setAllPlayersToNullNodes();
    }
    return false;
  }
}
