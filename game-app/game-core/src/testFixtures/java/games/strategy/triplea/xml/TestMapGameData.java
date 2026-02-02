package games.strategy.triplea.xml;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import games.strategy.triplea.delegate.TestDelegate;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import org.triplea.util.Version;

/** The available maps for use during testing. */
public enum TestMapGameData {
  BIG_WORLD_1942("big_world_1942_test.xml"),

  BIG_WORLD_1942_V3("Big_World_1942_v3rules.xml"),

  IRON_BLITZ("iron_blitz_test.xml"),

  LHTR("lhtr_test.xml"),

  PACIFIC_INCOMPLETE("pacific_incomplete_test.xml"),

  PACT_OF_STEEL_2("pact_of_steel_2_test.xml"),

  REVISED("revised_test.xml"),

  VICTORY_TEST("victory_test.xml"),
  VICTORY_TEST_SHOULD_SAVE_UP_FOR_A_FLEET("victory_test_shouldSaveUpForAFleet.xml"),

  WW2V3_1941("ww2v3_1941_test.xml"),

  WW2V3_1942("ww2v3_1942_test.xml"),

  GLOBAL1940("ww2_g40_balanced.xml"),

  TEST("Test.xml"),

  DELEGATE_TEST("DelegateTest.xml"),

  GAME_EXAMPLE("GameExample.xml"),

  TWW("Total_World_War_Dec1941.xml"),

  MINIMAP("minimap.xml"),

  NAPOLEONIC_EMPIRES("Napoleonic_Empires.xml"),
  ;

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
   * @throws RuntimeException If an error occurs while loading the map.
   */
  public GameData getGameData() {
    final Path mapUri;
    try {
      mapUri = Path.of(getClass().getClassLoader().getResource(fileName).toURI());
    } catch (final URISyntaxException e) {
      throw new IllegalStateException("Can't find " + fileName, e);
    }

    return GameParser.parse(
            mapUri,
            new XmlGameElementMapper(
                Map.of("TestDelegate", TestDelegate::new),
                Map.of("TestAttachment", TestAttachment::new)),
            new Version("2.0.0"),
            false)
        .orElseThrow(() -> new IllegalStateException("Error parsing: " + mapUri));
  }
}
