package games.strategy.triplea.xml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;

/**
 * The available maps for use during testing.
 */
public enum TestMapGameData {
  BIG_WORLD_1942("big_world_1942_test.xml"),

  IRON_BLITZ("iron_blitz_test.xml"),

  LHTR("lhtr_test.xml"),

  PACIFIC_INCOMPLETE("pacific_incomplete_test.xml"),

  PACT_OF_STEEL_2("pact_of_steel_2_test.xml"),

  REVISED("revised_test.xml"),

  VICTORY_TEST("victory_test.xml"),

  WW2V3_1941("ww2v3_1941_test.xml"),

  WW2V3_1942("ww2v3_1942_test.xml"),

  GLOBAL1940("ww2_g40_balanced.xml"),

  TEST("Test.xml"),

  DELEGATE_TEST("DelegateTest.xml"),

  GAME_EXAMPLE("GameExample.xml"),

  TWW("Total_World_War_Dec1941.xml");

  private final String fileName;

  TestMapGameData(final String value) {
    this.fileName = value;
  }

  @Override
  public String toString() {
    return fileName;
  }

  /**
   * Gets the game data for the associated map.
   *
   * @return The game data for the associated map.
   *
   * @throws Exception If an error occurs while loading the map.
   */
  public GameData getGameData() throws Exception {
    try (InputStream is = new FileInputStream(Paths.get("src", "test", "resources", fileName).toFile())) {
      return GameParser.parse("game name", is);
    }
  }
}
