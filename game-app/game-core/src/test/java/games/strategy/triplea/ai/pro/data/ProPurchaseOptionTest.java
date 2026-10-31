package games.strategy.triplea.ai.pro.data;

import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProPurchaseOptionTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();
  private final GamePlayer russia = russians(data);
  private final List<Unit> localInfantry = infantry(data).createTemp(1, russia);

  private ProPurchaseOption option(final String ruleName, final UnitType unitType) {
    return new ProPurchaseOption(
        data.getProductionRuleList().getProductionRule(ruleName), unitType, russia, data);
  }

  @Test
  void defenseEfficiencyWithoutAttackSupportDropsArtillerySupportCredit() {
    final ProPurchaseOption artillery = option("buyArtillery", artillery(data));

    assertThat(
            artillery.getDefenseEfficiencyWithoutAttackSupport(1, data, localInfantry, List.of()))
        .isLessThan(artillery.getDefenseEfficiency(1, data, localInfantry, List.of()));
  }

  @Test
  void defenseEfficiencyWithoutAttackSupportKeepsUnitsThatGiveNoAttackSupport() {
    final ProPurchaseOption infantry = option("buyInfantry", infantry(data));

    assertThat(infantry.getDefenseEfficiencyWithoutAttackSupport(1, data, localInfantry, List.of()))
        .isCloseTo(infantry.getDefenseEfficiency(1, data, localInfantry, List.of()), within(1e-9));
  }
}
