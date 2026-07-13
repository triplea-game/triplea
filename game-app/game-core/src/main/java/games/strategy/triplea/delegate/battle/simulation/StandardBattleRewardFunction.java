package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** Deterministic material-swing and terminal reward from the attacking side's perspective. */
public final class StandardBattleRewardFunction implements BattleRewardFunction {
  private final BattleRewardConfig config;

  public StandardBattleRewardFunction(final BattleRewardConfig config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public double reward(
      final BattleObservation before,
      final BattleObservation after,
      final BattleScenarioStep scenarioStep) {
    Objects.requireNonNull(before);
    Objects.requireNonNull(after);
    Objects.requireNonNull(scenarioStep);

    final int offenseLosses = unitCount(before.offense()) - unitCount(after.offense());
    final int defenseLosses = unitCount(before.defense()) - unitCount(after.defense());
    double reward = scenarioStep.reward();
    reward += Math.max(0, defenseLosses) * config.enemyUnitDestroyed();
    reward += Math.max(0, offenseLosses) * config.friendlyUnitLost();

    if (after.over()) {
      final boolean offenseAlive = unitCount(after.offense()) > 0;
      final boolean defenseAlive = unitCount(after.defense()) > 0;
      if (offenseAlive && !defenseAlive) {
        reward += config.victory();
      } else if (!offenseAlive && defenseAlive) {
        reward += config.defeat();
      } else {
        reward += config.draw();
      }
    }
    return reward;
  }

  private static int unitCount(final List<UnitGroupObservation> groups) {
    return groups.stream().mapToInt(UnitGroupObservation::count).sum();
  }
}
