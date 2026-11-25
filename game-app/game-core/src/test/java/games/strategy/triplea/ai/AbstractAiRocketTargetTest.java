package games.strategy.triplea.ai;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractAiRocketTargetTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final AbstractAi ai = new WeakAi("ai");

  @Test
  void picksHighestProductionTerritoryRegardlessOfOrder() {
    final Territory germany = territory("Germany", gameData);
    final Territory westernEurope = territory("Western Europe", gameData);
    assertThat(TerritoryAttachment.getProduction(germany))
        .isGreaterThan(TerritoryAttachment.getProduction(westernEurope));

    assertThat(ai.whereShouldRocketsAttack(List.of(westernEurope, germany), null))
        .isSameAs(germany);
    assertThat(ai.whereShouldRocketsAttack(List.of(germany, westernEurope), null))
        .isSameAs(germany);
  }

  @Test
  void breaksProductionTiesByName() {
    final Territory a = territory("5 Sea Zone", gameData);
    final Territory b = territory("6 Sea Zone", gameData);
    assertThat(TerritoryAttachment.getProduction(a))
        .isEqualTo(TerritoryAttachment.getProduction(b));

    assertThat(ai.whereShouldRocketsAttack(List.of(b, a), null)).isSameAs(a);
  }

  @Test
  void noCandidatesMeansNoTarget() {
    assertThat(ai.whereShouldRocketsAttack(List.of(), null)).isNull();
  }
}
