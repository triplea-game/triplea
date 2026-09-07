package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.RetreatCheckpoint;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reference fixed/heuristic retreat preference; all-or-none except submerge. The three thresholds
 * mirror {@code DummyPlayer}'s per-player retreat fields ({@code DummyPlayer#retreatQuery}, lines
 * 124-185); {@code -1} disables an integer rule.
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
    if (checkpoint == RetreatCheckpoint.SUBMERGE) {
      return submerging(cohort);
    }
    return retreatsAtRoundEnd(cohort, state) ? new LinkedHashMap<>(cohort) : Map.of();
  }

  /**
   * The submergeable slice of the cohort. Whether it <em>may</em> submerge (no blocking enemy
   * destroyer) is relational and gated by {@code CombatRelations}, not this preference seam.
   */
  private static Map<CombatProfile, Integer> submerging(final Map<CombatProfile, Integer> cohort) {
    final Map<CombatProfile, Integer> withdrawn = new LinkedHashMap<>();
    for (final Map.Entry<CombatProfile, Integer> entry : cohort.entrySet()) {
      if (entry.getKey().flags().contains(CombatFlag.CAN_SUBMERGE)) {
        withdrawn.put(entry.getKey(), entry.getValue());
      }
    }
    return withdrawn;
  }

  /** Ports the round-end branch of {@code DummyPlayer#retreatQuery} field-for-field. */
  private boolean retreatsAtRoundEnd(
      final Map<CombatProfile, Integer> cohort, final BattleView state) {
    if (retreatAfterRound > -1 && state.round() >= retreatAfterRound) {
      return true;
    }
    if (!retreatWhenOnlyAirLeft && retreatAfterXUnitsLeft <= -1) {
      return false;
    }
    final int unitsLeft = total(cohort);
    if (retreatWhenOnlyAirLeft) {
      final int retreatNum = airLeft(cohort) + Math.max(0, retreatAfterXUnitsLeft);
      if (retreatNum >= unitsLeft) {
        return true;
      }
    }
    return retreatAfterXUnitsLeft > -1 && retreatAfterXUnitsLeft >= unitsLeft;
  }

  private static int total(final Map<CombatProfile, Integer> cohort) {
    return cohort.values().stream().mapToInt(Integer::intValue).sum();
  }

  private static int airLeft(final Map<CombatProfile, Integer> cohort) {
    return cohort.entrySet().stream()
        .filter(entry -> entry.getKey().domain() == Domain.AIR)
        .mapToInt(Map.Entry::getValue)
        .sum();
  }
}
