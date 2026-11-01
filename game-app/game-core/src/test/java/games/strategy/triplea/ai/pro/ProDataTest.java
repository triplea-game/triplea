package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProDataTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void hardBetaAxisPlayerLandsBehindSeaBattles() {
    final ProData proData = new ProData(true);
    proData.initializeSimulation(null, gameData, germans(gameData));

    assertThat(proData.canLandBehindSeaBattle()).isTrue();
  }

  @Test
  void hardBetaAlliedPlayerDoesNotLandBehindSeaBattles() {
    final ProData proData = new ProData(true);
    proData.initializeSimulation(null, gameData, british(gameData));

    assertThat(proData.canLandBehindSeaBattle()).isFalse();
  }

  @Test
  void stockHardAxisPlayerDoesNotLandBehindSeaBattles() {
    final ProData proData = new ProData(false);
    proData.initializeSimulation(null, gameData, germans(gameData));

    assertThat(proData.canLandBehindSeaBattle()).isFalse();
  }
}
