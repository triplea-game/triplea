package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import java.util.Map;

/** Reference dice + low-luck hit resolution. */
public class DiceHitRoller implements HitRoller {
  @Override
  public int roll(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final IRandomSource rng) {
    throw new UnsupportedOperationException("phase 1");
  }
}
