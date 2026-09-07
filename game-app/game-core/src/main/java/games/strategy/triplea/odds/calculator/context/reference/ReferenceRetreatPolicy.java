package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import java.util.Map;

/** Reference fixed/heuristic retreat preference; all-or-none except submerge. */
public class ReferenceRetreatPolicy implements RetreatPolicy {
  @Override
  public Map<CombatProfile, Integer> withdraw(
      final Map<CombatProfile, Integer> cohort, final BattleView state) {
    throw new UnsupportedOperationException("phase 1");
  }
}
