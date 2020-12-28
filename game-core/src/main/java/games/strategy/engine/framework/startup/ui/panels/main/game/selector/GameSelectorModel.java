package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.GameParsingValidation;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelector;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Model class that tracks the currently 'selected' game. This is the info that appears in the game
 * selector panel on the staging screens, eg: map, round, filename.
 */
@Slf4j
public class GameSelectorModel extends Observable implements GameSelector {

  private final Function<URI, Optional<GameData>> gameParser;

  @Nullable
  @Getter(onMethod_ = {@Override})
  private GameData gameData = null;

  @Getter private String gameName = "-";
  @Getter private String gameRound = "-";
  @Nullable private String fileName;
  @Getter private boolean canSelect = true;
  @Getter private boolean hostIsHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots from this
  // model
  @Setter @Getter private ClientModel clientModelForHostBots = null;

  public GameSelectorModel() {
    this.gameParser = GameParser::parse;
  }

  /**
   * Loads game data by parsing a given file.
   *
   * @return True if successfully loaded, otherwise false.
   */
  public boolean load(final File file) {
    Preconditions.checkArgument(
        file.exists(),
        "Programming error, expected file to have already been checked to exist: "
            + file.getAbsolutePath());

    // if the file name is xml, load it as a new game
    if (file.getName().toLowerCase().endsWith("xml")) {
      load(file.toURI());
      return true;
    } else {
      // try to load it as a saved game whatever the extension
      final GameData newData = GameDataManager.loadGame(file).orElse(null);
      if (newData == null) {
        return false;
      }
      newData.setSaveGameFileName(file.getName());
      this.fileName = file.getName();
      setGameData(newData);
      return true;
    }
  }

  public void load(final URI uri) {
    fileName = null;
    GameData gameData = null;
    if (uri != null) {
      gameData = parseAndValidate(uri);
      if (gameData != null && gameData.getGameName() == null) {
        gameData = null;
      }
    }
    setGameData(gameData);
    this.setDefaultGame(uri, gameData);
  }

  private void setDefaultGame(@Nullable final URI uri, @Nullable final GameData gameData) {
    if (gameData == null || uri == null) {
      ClientSetting.defaultGameName.resetValue();
      ClientSetting.defaultGameUri.resetValue();
    } else {
      ClientSetting.defaultGameName.setValue(gameData.getGameName());
      ClientSetting.defaultGameUri.setValue(uri.toString());
    }
    ClientSetting.flush();
  }

  private void resetDefaultGame() {
    setGameData(null);
    setDefaultGame(null, null);
  }

  @Nullable
  private GameData parseAndValidate(final URI uri) {
    final GameData gameData = gameParser.apply(uri).orElse(null);
    if (gameData == null) {
      return null;
    }
    final List<String> validationErrors = new GameParsingValidation(gameData).validate();

    if (validationErrors.isEmpty()) {
      return gameData;
    } else {
      log.error(
          "Validation errors parsing map: {}, errors:\n{}",
          uri,
          String.join("\n", validationErrors));
      return null;
    }
  }

  public void setCanSelect(final boolean canSelect) {
    this.canSelect = canSelect;
    notifyObs();
  }

  public void setIsHostHeadlessBot(final boolean isHostHeadlessBot) {
    this.hostIsHeadlessBot = isHostHeadlessBot;
    notifyObs();
  }

  /**
   * We don't have a game data (i.e. we are a remote player and the data has not been sent yet), but
   * we still want to display game info.
   */
  public void clearDataButKeepGameInfo(final String gameName, final String gameRound) {
    synchronized (this) {
      gameData = null;
      this.gameName = gameName;
      this.gameRound = gameRound;
    }
    notifyObs();
  }

  public String getFileName() {
    return Optional.ofNullable(fileName).orElse("-");
  }

  public void setGameData(final GameData data) {
    synchronized (this) {
      if (data == null) {
        gameName = gameRound = "-";
      } else {
        gameName = data.getGameName();
        gameRound = "" + data.getSequence().getRound();
      }
      gameData = data;
    }
    notifyObs();
  }

  private void notifyObs() {
    super.setChanged();
    super.notifyObservers(gameData);
    super.clearChanged();
  }

  /** Clears AI game over cache and loads default game in a new thread. */
  @Override
  public void onGameEnded() {
    // clear out ai cached properties (this ended up being the best place to put it,
    // as we have definitely left a game at this point)
    GameShutdownRegistry.runShutdownActions();
    new Thread(this::loadDefaultGameSameThread).start();
  }

  /**
   * Runs the load default game logic in same thread. Default game is the one that we loaded on
   * startup.
   */
  public void loadDefaultGameSameThread() {
    ClientSetting.defaultGameUri
        .getValue()
        .filter(Predicate.not(String::isBlank))
        .filter(GameSelectorModel::gameUriExistsOnFileSystem)
        .map(URI::create)
        .ifPresentOrElse(this::load, this::resetDefaultGame);
  }

  @SuppressWarnings("ReturnValueIgnored")
  private static boolean gameUriExistsOnFileSystem(final String gameUri) {
    final URI uri = URI.create(gameUri);
    if (uri.getScheme() == null) {
      return false;
    }
    final Path realPath = getDefaultGameRealPath(uri);

    // starts with check is because we don't want to load a game file by default that is not within
    // the map folders. (ie: if a previous version of triplea was using running a game within its
    // root folder, we shouldn't open it)

    return realPath.startsWith(ClientFileSystemHelper.getUserRootFolder().toPath())
        && realPath.toFile().exists();
  }

  /**
   * Determine the real path of the default game.
   *
   * <p>A default game from a zip file will point to the file inside of the zip file. So, this
   * method will find the location of the zip file itself.
   */
  private static Path getDefaultGameRealPath(final URI defaultGame) {
    // The file system of the URI needs to be created before Path.of can be called.
    // So, first see if the file system is already created and if that throws
    // FileSystemNotFoundException, then try and create it.
    try {
      FileSystems.getFileSystem(defaultGame);
    } catch (final FileSystemNotFoundException notFoundException) {
      try {
        FileSystems.newFileSystem(defaultGame, Map.of());
      } catch (final IOException ioException) {
        // just ignore this error as Path.of() will throw a better error if the file is unable to
        // be read
      }
    } catch (final IllegalArgumentException illegalArgumentException) {
      // just ignore this error as Path.of() will throw a better error if the file is unable to
      // be read
    }
    final Path defaultGamePath = Path.of(defaultGame);
    if (defaultGamePath.getFileSystem().getFileStores().iterator().next().type().equals("zipfs")) {
      return Paths.get(defaultGamePath.getFileSystem().getFileStores().iterator().next().name());
    }
    return defaultGamePath;
  }
}
