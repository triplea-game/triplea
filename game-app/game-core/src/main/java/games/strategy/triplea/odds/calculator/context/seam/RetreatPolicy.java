package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.RetreatCheckpoint;
import java.util.Map;

/**
 * Preference only: per checkpoint and cohort, the per-profile withdrawal counts. Partial-capable
 * seam; v1 impls are all-or-none except submerge.
 */
public interface RetreatPolicy {
  Map<CombatProfile, Integer> withdraw(
      Map<CombatProfile, Integer> cohort, RetreatCheckpoint checkpoint, BattleView state);
}
