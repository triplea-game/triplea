package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import java.util.List;
import java.util.Map;

/**
 * Turns base counts into per-round evaluated strengths, allocated once force-wide. {@code side}
 * scopes which rules apply — a rule's {@code side} is fixed, so offense support never leaks to the
 * defending force.
 */
public interface SupportResolver {
  Map<CombatProfile, Integer> resolve(
      Force force, Force enemy, Side side, List<SupportRule> rules, int round);
}
