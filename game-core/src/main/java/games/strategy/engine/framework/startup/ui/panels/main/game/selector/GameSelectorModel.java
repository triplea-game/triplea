package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelector;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.GameChooserModel;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Observable;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * Model class that tracks the currently 'selected' game. This is the info that appears in the game
 * selector panel on the staging screens, eg: map, round, filename.
 */
@Log
public class GameSelectorModel extends Observable implements GameSelector {
  @Nullable
  @Getter(onMethod_ = {@Override})
  private GameData gameData = null;

  @Getter private String gameName = "";
  @Getter private String gameVersion = "";
  @Getter private String gameRound = "";
  @Nullable private String fileName;
  @Getter private boolean canSelect = true;
  @Getter private boolean hostIsHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots from this
  // model
  @Setter @Getter private ClientModel clientModelForHostBots = null;

  public GameSelectorModel() {
    resetGameDataToNull();
  }

  public void resetGameDataToNull() {
    load(null, null);
  }

  public void load(final @Nullable GameData data, final @Nullable String fileName) {
    setGameData(data);
    this.fileName = fileName;
    if (data != null) {
      log.info("Loaded game: " + data.getGameName() + ", in file: " + fileName);
    }
  }

  public void load(final GameChooserEntry entry) {
    fileName = null;
    if (entry == null
        || entry.getGameData() == null
        || entry.getGameData().getGameName() == null
        || entry.getUri() == null) {
      ClientSetting.defaultGameName.resetValue();
      ClientSetting.defaultGameUri.resetValue();
    } else {
      setGameData(entry.getGameData());
      ClientSetting.defaultGameName.setValue(entry.getGameData().getGameName());
      ClientSetting.defaultGameUri.setValue(entry.getUri().toString());
    }
    ClientSetting.flush();
  }

  /**
   * Loads game data by parsing a given file.
   *
   * @throws Exception If file parsing is successful and an internal {@code GameData} was set.
   */
  public void load(final File file) throws Exception {
    Preconditions.checkArgument(
        file.exists(),
        "Programming error, expected file to have already been checked to exist: "
            + file.getAbsolutePath());

    final GameData newData;
    // if the file name is xml, load it as a new game
    if (file.getName().toLowerCase().endsWith("xml")) {
      try (InputStream inputStream = new FileInputStream(file)) {
        newData = GameParser.parse(file.getAbsolutePath(), inputStream);
      }
    } else {
      // try to load it as a saved game whatever the extension
      newData = GameDataManager.loadGame(file);
      newData.setSaveGameFileName(file.getName());
    }
    load(newData, file.getName());
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
  public void clearDataButKeepGameInfo(
      final String gameName, final String gameRound, final String gameVersion) {
    synchronized (this) {
      gameData = null;
      this.gameName = gameName;
      this.gameRound = gameRound;
      this.gameVersion = gameVersion;
    }
    notifyObs();
  }

  public String getFileName() {
    return Optional.ofNullable(fileName).orElse("-");
  }

  public void setGameData(final GameData data) {
    synchronized (this) {
      if (data == null) {
        gameName = gameRound = gameVersion = "-";
      } else {
        gameName = data.getGameName();
        gameRound = "" + data.getSequence().getRound();
        gameVersion = data.getGameVersion().toString();
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
    ProAi.gameOverClearCache();
    new Thread(this::loadDefaultGameSameThread).start();
  }

  /**
   * Runs the load default game logic in same thread. Default game is the one that we loaded on
   * startup.
   */
  public void loadDefaultGameSameThread() {
    final String userPreferredDefaultGameUri = ClientSetting.defaultGameUri.getValue().orElse("");

    // we don't want to load a game file by default that is not within the map folders we can load.
    // (ie: if a previous
    // version of triplea was using running a game within its root folder, we shouldn't open it)
    GameChooserEntry selectedGame;
    final String user = ClientFileSystemHelper.getUserRootFolder().toURI().toString();
    if (!userPreferredDefaultGameUri.isEmpty() && userPreferredDefaultGameUri.contains(user)) {
      // if the user has a preferred URI, then we load it, and don't bother parsing or doing
      // anything with the whole
      // game model list
      try {
        final URI defaultUri = new URI(userPreferredDefaultGameUri);
        selectedGame = GameChooserEntry.newInstance(defaultUri);
      } catch (final Exception e) {
        resetToFactoryDefault();
        selectedGame = selectByName();
        if (selectedGame == null) {
          return;
        }
      }
      if (!selectedGame.isGameDataLoaded()) {
        try {
          selectedGame.fullyParseGameData();
        } catch (final GameParseException e) {
          resetToFactoryDefault();
          loadDefaultGameSameThread();
          return;
        }
      }
    } else {
      resetToFactoryDefault();
      selectedGame = selectByName();
      if (selectedGame == null) {
        return;
      }
    }
    load(selectedGame);
  }

  private static void resetToFactoryDefault() {
    ClientSetting.defaultGameUri.resetValue();
    ClientSetting.flush();
  }

  private static GameChooserEntry selectByName() {
    final String userPreferredDefaultGameName = ClientSetting.defaultGameName.getValueOrThrow();

    final GameChooserModel model = new GameChooserModel();
    GameChooserEntry selectedGame = model.findByName(userPreferredDefaultGameName).orElse(null);

    if (selectedGame == null && !model.isEmpty()) {
      selectedGame = model.get(0);
    }
    if (selectedGame == null) {
      return null;
    }
    if (!selectedGame.isGameDataLoaded()) {
      try {
        selectedGame.fullyParseGameData();
      } catch (final GameParseException e) {
        model.removeEntry(selectedGame);
        resetToFactoryDefault();
        return null;
      }
    }
    return selectedGame;
  }
}
