package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.aFileWithSize;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

  @ParameterizedTest
  @CsvSource({
    "map_making_tutorial,map/games/Test1.xml",
    "minimap,map/games/minimap.xml",
    "world_war_ii_revised,map/games/ww2v2.xml",
    "pacific_challenge,map/games/Pacific_Theater_Solo_Challenge.xml",
    "imperialism_1974_board_game,map/games/imperialism_1974_board_game.xml",
  })
  void testSaveGame(String mapName, String mapXmlPath) throws Exception {
    GameSelectorModel gameSelector = GameTestUtils.loadGameFromURI(mapName, mapXmlPath);
    ServerGame game = GameTestUtils.setUpGameWithAis(gameSelector);
    Path saveFile = Files.createTempFile("save", GameDataFileUtils.getExtension());
    game.saveGame(saveFile);
    assertThat(saveFile.toFile(), is(not(aFileWithSize(0))));
  }
}
