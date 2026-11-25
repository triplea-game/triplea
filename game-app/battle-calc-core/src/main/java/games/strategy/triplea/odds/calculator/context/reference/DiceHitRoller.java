package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
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
      final CombatProfile profile = entry.getKey();
      final int strength = strengthOf(profile, ctx);
      final int count = entry.getValue();
      final int rolls = profile.rolls();
      if (rolls > 1 && profile.flags().contains(CombatFlag.CHOOSE_BEST_ROLL)) {
        hits += chooseBestRollHits(count, rolls, strength, ctx, rng);
      } else {
        final int dice = count * rolls;
        for (int i = 0; i < dice; i++) {
          if (rng.getRandom(ctx.diceSides(), ANNOTATION) < strength) {
            hits++;
          }
        }
      }
    }
    return hits;
  }

  /**
   * Best-of-rolls counting: each body still rolls all its dice but scores at most one hit, on its
   * best die — the LHTR heavy-bomber rule (engine {@code RolledDice#getDiceForChooseBestRoll}). All
   * the dice are drawn so the random stream advances the same as the all-rolls path.
   */
  private static int chooseBestRollHits(
      final int count,
      final int rolls,
      final int strength,
      final FireContext ctx,
      final RandomSource rng) {
    int hits = 0;
    for (int unit = 0; unit < count; unit++) {
      boolean hit = false;
      for (int r = 0; r < rolls; r++) {
        if (rng.getRandom(ctx.diceSides(), ANNOTATION) < strength) {
          hit = true;
        }
      }
      if (hit) {
        hits++;
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
      final CombatProfile profile = entry.getKey();
      final int rolls = profile.rolls();
      final int count = entry.getValue();
      final int rawStrength = strengthOf(profile, ctx);
      // Engine PowerCalculator#getValue short-circuits a unit whose raw strength or roll count is
      // zero to no power — the best-of-rolls bonus below never revives a zero-strength unit.
      if (rawStrength == 0 || rolls == 0) {
        continue;
      }
      // Engine StrengthValue floors each unit's strength at 0 then caps it at diceSides before
      // summing, so an enemy-debuffed unit contributes nothing rather than subtracting from the
      // vector's power, and no unit beats one guaranteed hit per die.
      final int strength = Math.min(Math.max(rawStrength, 0), ctx.diceSides());
      if (rolls > 1 && profile.flags().contains(CombatFlag.CHOOSE_BEST_ROLL)) {
        // Under low luck the engine (PowerCalculator) approximates best-of-rolls as the unit's
        // strength plus one bonus per extra roll, capped at diceSides.
        final int bonus = Math.max(1, ctx.diceSides() / 6);
        power += Math.min(strength + bonus * (rolls - 1), ctx.diceSides()) * count;
      } else {
        power += strength * rolls * count;
      }
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
