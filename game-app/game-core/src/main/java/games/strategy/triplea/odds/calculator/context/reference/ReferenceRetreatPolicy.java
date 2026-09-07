package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.RetreatCheckpoint;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import java.util.Map;

/**
 * Reference fixed/heuristic retreat preference; all-or-none except submerge. The three thresholds
 * mirror {@code DummyPlayer}'s per-player retreat fields; a negative threshold disables that rule.
 */
public class ReferenceRetreatPolicy implements RetreatPolicy {
  private final int retreatAfterRound;
  private final int retreatAfterXUnitsLeft;
  private final boolean retreatWhenOnlyAirLeft;

  public ReferenceRetreatPolicy(
      final int retreatAfterRound,
      final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft) {
    this.retreatAfterRound = retreatAfterRound;
    this.retreatAfterXUnitsLeft = retreatAfterXUnitsLeft;
    this.retreatWhenOnlyAirLeft = retreatWhenOnlyAirLeft;
  }

  @Override
  public Map<CombatProfile, Integer> withdraw(
      final Map<CombatProfile, Integer> cohort,
      final RetreatCheckpoint checkpoint,
      final BattleView state) {
    throw new UnsupportedOperationException("phase 1");
  }
}
