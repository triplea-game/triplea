package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.aFileWithSize;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.ServerGame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks that no error is encountered when saving a game on several different maps. This test
 * downloads the maps in question and starts an all-AI game on each of them, before saving.
 *
 * <p>Note: A variety of maps are used to ensure different engine features are exercised since
 * object serialization may run into errors that depend on the state of the object graph. For
 * example, if there's a non-null reference to a non-serializable object.
 */
class GameSaveTest {
  @BeforeAll
  public static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  /** Validates that we can properly create a save game file without any errors. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "Test1.xml",
        "minimap.xml",
        "ww2v2.xml",
        "Pacific_Theater_Solo_Challenge.xml",
        "imperialism_1974_board_game.xml"
      })
  void testSaveGame(String mapXmlPath) throws Exception {
    ServerGame game = GameTestUtils.setUpGameWithAis(mapXmlPath);
    Path saveFile = Files.createTempFile("save", GameDataFileUtils.getExtension());
    game.saveGame(saveFile);
    assertThat(saveFile.toFile(), is(not(aFileWithSize(0))));
  }
}
