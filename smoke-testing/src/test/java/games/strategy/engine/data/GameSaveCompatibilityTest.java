package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

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

    assertThat(gameData.getAttachmentOrderAndValues(), is(notNullValue()));
    assertThat(gameData.getAllianceTracker().getAlliances(), is(notNullValue()));
    assertThat(gameData.getBattleRecordsList(), is(notNullValue()));
    assertThat(gameData.getDelegates(), is(notNullValue()));
    assertThat(gameData.getDiceSides(), is(notNullValue()));
    assertThat(gameData.getGameLoader(), is(notNullValue()));
    assertThat(gameData.getGameName(), is(notNullValue()));
    assertThat(gameData.getHistory().getLastNode(), is(notNullValue()));
    assertThat(gameData.getMap().getTerritories(), is(notNullValue()));
    assertThat(gameData.getPlayerList().getPlayers(), is(notNullValue()));
    assertThat(
        gameData.getProductionFrontierList().getProductionFrontierNames(), is(notNullValue()));
    assertThat(gameData.getProductionRuleList().getProductionRules(), is(notNullValue()));
    assertThat(gameData.getProperties(), is(notNullValue()));
    assertThat(gameData.getRelationshipTracker(), is(notNullValue()));
    assertThat(gameData.getRelationshipTypeList().getAllRelationshipTypes(), is(notNullValue()));
    assertThat(gameData.getRepairFrontierList().getRepairFrontierNames(), is(notNullValue()));
    assertThat(gameData.getResourceList().getResources(), is(notNullValue()));
    assertThat(gameData.getSaveGameFileName(), is(notNullValue()));
    assertThat(gameData.getSequence().getRound(), is(notNullValue()));
    assertThat(gameData.getSequence().getStep(), is(notNullValue()));
    assertThat(gameData.getTechnologyFrontier().getTechs(), is(notNullValue()));
    assertThat(gameData.getTerritoryEffectList(), is(notNullValue()));
    assertThat(gameData.getUnits().getUnits(), is(notNullValue()));
    assertThat(gameData.getUnitTypeList().getAllUnitTypes(), is(notNullValue()));
  }

  @SuppressWarnings("unused")
  static Collection<Path> loadSaveGames() throws IOException {
    return TestDataFileLister.listFilesInTestResourcesDirectory("save-games");
  }
}
