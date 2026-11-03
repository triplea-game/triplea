package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.rolls;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.model.Phase;
import games.strategy.triplea.odds.calculator.context.reference.DiceHitRoller;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.vector.VectorizedHitRoller;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Pins the stronger claim the contract test cannot: {@link VectorizedHitRoller} is not merely
 * distribution-equivalent to {@link DiceHitRoller} but scores the <em>identical</em> hit count for
 * the same firing vector and the same die stream — the batched draw consumes dice in the same
 * profile order as the per-die loop, so replaying one stream through both must agree.
 */
class VectorizedHitRollerStreamEquivalenceTest {

  private static final int DICE_SIDES = 6;

  private final HitRoller reference = new DiceHitRoller();
  private final HitRoller vectorized = new VectorizedHitRoller();

  @Test
  void batchedDrawScoresIdenticalHitsToPerDieRollsAcrossManyStreams() {
    // A mixed vector: single- and multi-roll bodies at different strengths, plus a choose-best-roll
    // body whose per-body grouping the batch must segment the same way, so the die-to-profile
    // mapping actually matters and a mis-segmented batch would diverge.
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile artillery = land("artillery", 2, 2, 1);
    final CombatProfile fighter = rolls(air("fighter", 3, 4, 1), 2);
    final CombatProfile bomber =
        withFlags(rolls(air("bomber", 4, 4, 1), 2), CombatFlag.CHOOSE_BEST_ROLL);
    final Map<CombatProfile, Integer> firing =
        Map.of(infantry, 4, artillery, 2, fighter, 3, bomber, 2);
    final FireContext offense = new FireContext(1, Phase.GENERAL, true, false, DICE_SIDES);
    final int totalDice = 4 + 2 + 3 * 2 + 2 * 2;

    final Random random = new Random(4242);
    int nonZeroTrials = 0;
    for (int trial = 0; trial < 500; trial++) {
      final int[] stream = random.ints(totalDice, 0, DICE_SIDES).toArray();
      final int referenceHits = reference.roll(firing, offense, FakeRandomSource.scripted(stream));
      final int vectorizedHits =
          vectorized.roll(firing, offense, FakeRandomSource.scripted(stream));
      assertThat(vectorizedHits).isEqualTo(referenceHits);
      if (referenceHits > 0) {
        nonZeroTrials++;
      }
    }
    // Guards against a vacuous pass where every stream happened to score zero on both sides.
    assertThat(nonZeroTrials).isPositive();
  }
}
