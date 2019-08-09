package games.strategy.engine.framework.startup.mc;

import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.GameChooserModel;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Observable;
import java.util.logging.Level;
import javax.annotation.Nullable;
import lombok.extern.java.Log;

/**
 * Model class that tracks the currently 'selected' game. This is the info that appears in the game
 * selector panel on the staging screens, eg: map, round, filename.
 */
@Log
public class GameSelectorModel extends Observable {
  private GameData gameData = null;
  private String gameName = "";
  private String gameVersion = "";
  private String gameRound = "";
  @Nullable private String fileName;
  private boolean canSelect = true;
  private boolean isHostHeadlessBot = false;
  // just for host bots, so we can get the actions for loading/saving games on the bots from this
  // model
  private ClientModel clientModelForHostBots = null;

  public GameSelectorModel() {
    resetGameDataToNull();
  }

  public void resetGameDataToNull() {
    load(null, null);
  }

  public void load(final @Nullable GameData data, final @Nullable String fileName) {
    setGameData(data);
    this.fileName = fileName;
  }

  public void load(final GameChooserEntry entry) {
    fileName = null;
    setGameData(entry.getGameData());
    if (entry.getGameData() != null) {
      ClientSetting.defaultGameName.setValue(entry.getGameData().getGameName());
    }
    ClientSetting.defaultGameUri.setValue(entry.getUri().toString());
    ClientSetting.flush();
  }

  /**
   * Loads game data by parsing a given file.
   *
   * @return True if file parsing was successful and an internal {@code GameData} was set. Otherwise
   *     returns false and internal {@code GameData} is null.
   */
  public boolean load(final File file) {
    Preconditions.checkArgument(
        file.exists() && file.isFile(), "File should exist at: " + file.getAbsolutePath());

    final GameData newData;
    try {
      // if the file name is xml, load it as a new game
      if (file.getName().toLowerCase().endsWith("xml")) {
        try (InputStream inputStream = new FileInputStream(file)) {
          newData = GameParser.parse(file.getAbsolutePath(), inputStream);
        }
      } else {
        // try to load it as a saved game whatever the extension
        newData = GameDataManager.loadGame(file);
      }
      if (newData != null) {
        load(newData, file.getName());
      }
      return (newData != null);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error loading game file: " + file.getAbsolutePath(), e);
      return false;
    }
  }

  public GameData getGameData(final InputStream input) {
    try {
      return GameDataManager.loadGame(input);
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to load game", e);
      return null;
    }
  }

  public synchronized GameData getGameData() {
    return gameData;
  }

  synchronized boolean isSavedGame() {
    return fileName != null;
  }

  void setCanSelect(final boolean canSelect) {
    synchronized (this) {
      this.canSelect = canSelect;
    }
    notifyObs();
  }

  public synchronized boolean canSelect() {
    return canSelect;
  }

  void setIsHostHeadlessBot(final boolean isHostHeadlessBot) {
    synchronized (this) {
      this.isHostHeadlessBot = isHostHeadlessBot;
    }
    notifyObs();
  }

  public synchronized boolean isHostHeadlessBot() {
    return isHostHeadlessBot;
  }

  synchronized void setClientModelForHostBots(final ClientModel clientModel) {
    clientModelForHostBots = clientModel;
  }

  public synchronized ClientModel getClientModelForHostBots() {
    return clientModelForHostBots;
  }

  /**
   * We don't have a game data (i.e. we are a remote player and the data has not been sent yet), but
   * we still want to display game info.
   */
  void clearDataButKeepGameInfo(
      final String gameName, final String gameRound, final String gameVersion) {
    synchronized (this) {
      gameData = null;
      this.gameName = gameName;
      this.gameRound = gameRound;
      this.gameVersion = gameVersion;
    }
    notifyObs();
  }

  public synchronized String getFileName() {
    return (fileName == null) ? "-" : fileName;
  }

  public synchronized String getGameName() {
    return gameName;
  }

  public synchronized String getGameRound() {
    return gameRound;
  }

  public synchronized String getGameVersion() {
    return gameVersion;
  }

  void setGameData(final GameData data) {
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
  public void loadDefaultGameNewThread() {
    // clear out ai cached properties (this ended up being the best place to put it, as we have
    // definitely left a game
    // at this point)
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

    if (selectedGame == null && model.size() > 0) {
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
