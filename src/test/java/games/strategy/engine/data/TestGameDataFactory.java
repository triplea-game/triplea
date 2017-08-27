package games.strategy.engine.data;

import games.strategy.triplea.TripleA;
import games.strategy.util.Version;

/**
 * A factory for creating instances of {@code GameData} for use in tests.
 */
public final class TestGameDataFactory {
  private TestGameDataFactory() {}

  /**
   * Creates a new {@code GameData} instance that is valid in all respects.
   *
   * @return A new valid {@code GameData} instance.
   */
  public static GameData newValidGameData() {
    final GameData gameData = new GameData();
    gameData.setDiceSides(42);
    gameData.setGameLoader(new TripleA());
    gameData.setGameName("name");
    gameData.setGameVersion(new Version(1, 2, 3, 4));
    // TODO: initialize other attributes
    return gameData;
  }
}
