package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;
import java.util.List;
import java.util.Map;

/** Reference support allocation honoring the engine's consume/sort order. */
public class ReferenceSupportResolver implements SupportResolver {
  @Override
  public Map<CombatProfile, Integer> resolve(
      final Force side, final Force otherSide, final List<SupportRule> rules, final int round) {
    throw new UnsupportedOperationException("phase 1");
  }
}
