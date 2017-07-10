package games.strategy.engine.display;

import games.strategy.engine.data.GameData;

public class DefaultDisplayBridge implements IDisplayBridge {
  private final GameData m_gameData;

  /**
   * Constructs a DefaultDisplayBridge.
   */
  public DefaultDisplayBridge(final GameData gameData) {
    super();
    m_gameData = gameData;
  }

  @Override
  public GameData getGameData() {
    return m_gameData;
  }
}
