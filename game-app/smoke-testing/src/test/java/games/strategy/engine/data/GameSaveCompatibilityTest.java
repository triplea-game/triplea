package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.GameDataManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GameSaveCompatibilityTest {

  @ParameterizedTest
  @MethodSource
  void loadSaveGames(final Path saveGame) throws Exception {
    final GameData gameData;
    try (InputStream inputStream = Files.newInputStream(saveGame)) {
      gameData = GameDataManager.loadGame(inputStream).orElseThrow();
    }

    assertThat(gameData.getAttachmentOrderAndValues()).isNotNull();
    assertThat(gameData.getAllianceTracker().getAlliances()).isNotNull();
    assertThat(gameData.getBattleRecordsList()).isNotNull();
    assertThat(gameData.getDelegates()).isNotNull();
    assertThat(gameData.getDiceSides()).isNotNull();
    assertThat(gameData.getGameLoader()).isNotNull();
    assertThat(gameData.getGameName()).isNotNull();
    assertThat(gameData.getHistory().getLastNode()).isNotNull();
    assertThat(gameData.getMap().getTerritories()).isNotNull();
    assertThat(gameData.getPlayerList().getPlayers()).isNotNull();
    assertThat(gameData.getProductionFrontierList().getProductionFrontierNames()).isNotNull();
    assertThat(gameData.getProductionRuleList().getProductionRules()).isNotNull();
    assertThat(gameData.getProperties()).isNotNull();
    assertThat(gameData.getRelationshipTracker()).isNotNull();
    assertThat(gameData.getRelationshipTypeList().getAllRelationshipTypes()).isNotNull();
    assertThat(gameData.getRepairFrontierList().getRepairFrontierNames()).isNotNull();
    assertThat(gameData.getResourceList().getResources()).isNotNull();
    assertThat(gameData.getSaveGameFileName()).isNotNull();
    assertThat(gameData.getSequence().getRound()).isNotNull();
    assertThat(gameData.getSequence().getStep()).isNotNull();
    assertThat(gameData.getTechnologyFrontier().getTechs()).isNotNull();
    assertThat(gameData.getTerritoryEffectList()).isNotNull();
    assertThat(gameData.getUnits().getUnits()).isNotNull();
    assertThat(gameData.getUnitTypeList().getAllUnitTypes()).isNotNull();
  }

  @SuppressWarnings("unused")
  static Collection<Path> loadSaveGames() throws IOException {
    return TestDataFileLister.listFilesInTestClasspathDir("save-games");
  }
}
