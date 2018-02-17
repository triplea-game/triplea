package games.strategy.engine.display;

import games.strategy.engine.data.GameData;

public class DefaultDisplayBridge implements IDisplayBridge {
  private final GameData gameData;

  /**
   * Constructs a DefaultDisplayBridge.
   */
  public DefaultDisplayBridge(final GameData gameData) {
    super();
    this.gameData = gameData;
  }

  @Override
  public GameData getGameData() {
    return gameData;
  }
}
