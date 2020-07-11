package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import games.strategy.engine.framework.GameDataManager;
import java.io.File;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GameSaveCompatibilityTest {

  @ParameterizedTest
  @MethodSource
  void loadSaveGames(final File saveGame) {
    assertDoesNotThrow(() -> GameDataManager.loadGame(saveGame));
  }

  @SuppressWarnings("unused")
  static Collection<File> loadSaveGames() {
    return TestDataFileLister.listFilesInTestResourcesDirectory("save-games");
  }
}
