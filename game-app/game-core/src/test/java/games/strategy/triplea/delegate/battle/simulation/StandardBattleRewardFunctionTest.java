package games.strategy.triplea.delegate.battle.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandardBattleRewardFunctionTest {
  @Test
  void rewardsMaterialSwingAndAttackerVictory() {
    final BattleRewardFunction rewardFunction =
        new StandardBattleRewardFunction(BattleRewardConfig.defaults());
    final BattleObservation before = observation(false, 2, 2);
    final BattleObservation after = observation(true, 1, 0);

    final double reward =
        rewardFunction.reward(before, after, new BattleScenarioStep(0, false, Map.of()));

    assertEquals(11.0, reward);
  }

  @Test
  void preservesScenarioRewardOnDrawWithoutMaterialChange() {
    final BattleRewardFunction rewardFunction = BattleRewardFunction.standard();
    final BattleObservation before = observation(false, 0, 0);
    final BattleObservation after = observation(true, 0, 0);

    final double reward =
        rewardFunction.reward(before, after, new BattleScenarioStep(0.75, false, Map.of()));

    assertEquals(0.75, reward);
  }

  private static BattleObservation observation(
      final boolean over, final int offenseCount, final int defenseCount) {
    return new BattleObservation(
        BattleObservation.CURRENT_SCHEMA_VERSION,
        "battle-id",
        "territory",
        1,
        2,
        over,
        false,
        true,
        "attacker",
        "defender",
        groups("attacker", offenseCount),
        groups("defender", defenseCount),
        List.of());
  }

  private static List<UnitGroupObservation> groups(final String owner, final int count) {
    return count == 0
        ? List.of()
        : List.of(new UnitGroupObservation(owner, "infantry", 0, BigDecimal.ZERO, count));
  }
}
