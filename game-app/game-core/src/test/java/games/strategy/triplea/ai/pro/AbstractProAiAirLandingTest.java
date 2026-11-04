package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractProAiAirLandingTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void landsWhereTheMostAlliedGroundStrengthIs() {
    final var unitedKingdom = territory("United Kingdom", gameData);
    final var germany = territory("Germany", gameData);

    assertThat(
            AbstractProAi.strongestAlliedGround(List.of(germany, unitedKingdom), british(gameData)))
        .isEqualTo(unitedKingdom);
    assertThat(
            AbstractProAi.strongestAlliedGround(List.of(unitedKingdom, germany), british(gameData)))
        .isEqualTo(unitedKingdom);
  }
}
