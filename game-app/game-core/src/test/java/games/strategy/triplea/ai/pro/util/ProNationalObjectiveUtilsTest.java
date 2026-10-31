package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProNationalObjectiveUtilsTest {
  // Germany's 5-PU objective needs 1 of Karelia S.S.R. and Caucasus, both Russian at the start.
  private final GameData gameData = TestMapGameData.WW2V3_1941.getGameData();
  private final Territory karelia = territory("Karelia S.S.R.", gameData);
  private final Territory brazil = territory("Brazil", gameData);

  @Test
  void territoryThatCompletesAnObjectiveIsWorthItsPus() {
    assertThat(ProNationalObjectiveUtils.values(germans(gameData))).containsEntry(karelia, 5.0);
  }

  @Test
  void territoryThatDeniesAnEnemyObjectiveIsWorthItsPus() {
    assertThat(ProNationalObjectiveUtils.values(russians(gameData))).containsEntry(karelia, 5.0);
  }

  @Test
  void territoryInNoObjectiveIsWorthNothing() {
    assertThat(ProNationalObjectiveUtils.values(germans(gameData))).doesNotContainKey(brazil);
  }

  @Test
  void stockHardIgnoresObjectives() {
    assertThat(ProNationalObjectiveUtils.value(new ProData(false), germans(gameData), karelia))
        .isZero();
  }
}
