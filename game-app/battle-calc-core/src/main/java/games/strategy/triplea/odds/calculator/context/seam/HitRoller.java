package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import java.util.Map;

/**
 * Turns an evaluated firing vector into hits; the seam where dice/low-luck/analytic/vector diverge.
 */
public interface HitRoller {
  int roll(Map<CombatProfile, Integer> firing, FireContext ctx, RandomSource rng);
}
