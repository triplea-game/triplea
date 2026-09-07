package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import java.util.List;
import java.util.Map;

/** Turns base counts into per-round evaluated strengths, allocated once force-wide. */
public interface SupportResolver {
  Map<CombatProfile, Integer> resolve(
      Force side, Force otherSide, List<SupportRule> rules, int round);
}
