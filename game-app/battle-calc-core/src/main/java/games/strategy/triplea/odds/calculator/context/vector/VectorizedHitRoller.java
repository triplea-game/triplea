package games.strategy.triplea.odds.calculator.context.vector;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.reference.DiceHitRoller;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;
import java.util.Map;

/**
 * A {@link HitRoller} that draws a firing vector's dice in one batched {@link
 * RandomSource#getRandom(int, int, String)} call and counts hits in a tight primitive-array loop,
 * rather than a call per die. The per-die {@code < strength} rule is unchanged, so it consumes dice
 * in the same order as {@link DiceHitRoller} and is stream-identical to it, not merely
 * distribution-equivalent.
 *
 * <p>Only the dice path is batched. Low-luck is already closed-form (one summed power plus a single
 * remainder die), so it is delegated to {@link DiceHitRoller} — there is no per-die loop to
 * collapse.
 */
public class VectorizedHitRoller implements HitRoller {

  private static final String ANNOTATION = "battle-calc hit roll";

  private final DiceHitRoller lowLuck = new DiceHitRoller();

  @Override
  public int roll(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final RandomSource rng) {
    return ctx.lowLuck() ? lowLuck.roll(firing, ctx, rng) : diceHits(firing, ctx, rng);
  }

  /**
   * Draws every die in the vector at once, then counts the ones below each profile's strength. The
   * dice are laid out profile by profile in the firing map's iteration order — the same order
   * {@link DiceHitRoller} draws them — so a given die value lands on the same profile in both
   * impls.
   */
  private int diceHits(
      final Map<CombatProfile, Integer> firing, final FireContext ctx, final RandomSource rng) {
    int totalDice = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : firing.entrySet()) {
      totalDice += entry.getValue() * entry.getKey().rolls();
    }
    if (totalDice == 0) {
      return 0;
    }
    final int[] draws = rng.getRandom(ctx.diceSides(), totalDice, ANNOTATION);
    int hits = 0;
    int die = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : firing.entrySet()) {
      final CombatProfile profile = entry.getKey();
      final int strength = strengthOf(profile, ctx);
      final int count = entry.getValue();
      final int rolls = profile.rolls();
      if (rolls > 1 && profile.flags().contains(CombatFlag.CHOOSE_BEST_ROLL)) {
        // Best-of-rolls: each body consumes all its dice but scores at most one hit, on its best
        // die — kept identical to DiceHitRoller so the two stay stream-equivalent.
        for (int unit = 0; unit < count; unit++) {
          boolean hit = false;
          for (int r = 0; r < rolls; r++) {
            if (draws[die++] < strength) {
              hit = true;
            }
          }
          if (hit) {
            hits++;
          }
        }
      } else {
        final int dice = count * rolls;
        for (int i = 0; i < dice; i++) {
          if (draws[die++] < strength) {
            hits++;
          }
        }
      }
    }
    return hits;
  }

  private static int strengthOf(final CombatProfile profile, final FireContext ctx) {
    return ctx.isOffense() ? profile.attack() : profile.defense();
  }
}
