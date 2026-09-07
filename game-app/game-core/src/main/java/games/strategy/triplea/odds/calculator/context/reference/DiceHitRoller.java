package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;
import java.util.Map;

/**
 * Reference {@link HitRoller}: rolls real dice (or low-luck buckets) for one evaluated firing
 * vector. A die hits when its value is strictly below the firing strength — attack on offense,
 * defense on defense — mirroring the engine's {@code < strength} rule.
 */
public class DiceHitRoller implements HitRoller {

  private static final String ANNOTATION = "battle-calc hit roll";

  @Override
  public int roll(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final RandomSource rng) {
    return ctx.lowLuck() ? lowLuckHits(firing, ctx, rng) : diceHits(firing, ctx, rng);
  }

  private int diceHits(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final RandomSource rng) {
    int hits = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : firing.entrySet()) {
      final int strength = strengthOf(entry.getKey(), ctx);
      final int dice = entry.getValue() * entry.getKey().rolls();
      for (int i = 0; i < dice; i++) {
        if (rng.getRandom(ctx.diceSides(), ANNOTATION) < strength) {
          hits++;
        }
      }
    }
    return hits;
  }

  /**
   * Low-luck folds the whole vector's strength into guaranteed hits plus a single fractional die:
   * the total power divided by the die sides is the floor of hits, and its remainder becomes one
   * extra die that hits only when rolled below the remainder (engine {@code BattleDelegate}: {@code
   * remainder > roll} ie {@code roll < remainder}). A zero remainder scores no extra die and draws
   * no random value.
   */
  private int lowLuckHits(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final RandomSource rng) {
    int power = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : firing.entrySet()) {
      // Engine caps each unit's low-luck strength at diceSides (StrengthValue) before summing —
      // a unit can't contribute better than one guaranteed hit per die.
      final int strength = Math.min(strengthOf(entry.getKey(), ctx), ctx.diceSides());
      power += strength * entry.getKey().rolls() * entry.getValue();
    }
    int hits = power / ctx.diceSides();
    final int remainder = power % ctx.diceSides();
    if (remainder > 0 && rng.getRandom(ctx.diceSides(), ANNOTATION) < remainder) {
      hits++;
    }
    return hits;
  }

  private static int strengthOf(final CombatProfile profile, final FireContext ctx) {
    return ctx.isOffense() ? profile.attack() : profile.defense();
  }
}
