package games.strategy.triplea.odds.calculator.context.reference;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.aa;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withMaxAaAttacks;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

/**
 * Pins the wired per-round AA dice cap ({@link AaFireCap#cappedFiring}) against the engine's {@code
 * AaPowerStrengthAndRolls} allocation: total AA dice never exceed the live air-target count, finite
 * guns take dice strongest-first up to their per-gun cap, and a single infinite gun fills whatever
 * targets the finite guns leave. Each case reads the returned firing vector — the dice a gun rolls
 * are its {@code count * rolls} — since that vector is exactly what the simulator hands the roller.
 */
class AaFireCapTest {

  private static final ToIntFunction<CombatProfile> BY_DEFENSE = CombatProfile::defense;

  @Test
  void infiniteGunFiresOneDiePerAirTarget() {
    final Map<CombatProfile, Integer> firing = Map.of(aa("aaGun", 0, 1, 1), 1);

    assertThat(totalDice(AaFireCap.cappedFiring(firing, 3, BY_DEFENSE))).isEqualTo(3);
  }

  @Test
  void finiteGunIsCappedAtItsPerGunLimitWhenAirIsPlentiful() {
    final Map<CombatProfile, Integer> firing = Map.of(withMaxAaAttacks(aa("aaGun", 0, 1, 1), 3), 1);

    assertThat(totalDice(AaFireCap.cappedFiring(firing, 5, BY_DEFENSE))).isEqualTo(3);
  }

  @Test
  void finiteGunFiresEveryDieWhenAirExactlyMatchesItsCap() {
    final Map<CombatProfile, Integer> firing = Map.of(withMaxAaAttacks(aa("aaGun", 0, 1, 1), 3), 1);

    assertThat(totalDice(AaFireCap.cappedFiring(firing, 3, BY_DEFENSE))).isEqualTo(3);
  }

  @Test
  void finiteGunIsCappedAtTheAirTargetCountWhenAirIsScarce() {
    final Map<CombatProfile, Integer> firing = Map.of(withMaxAaAttacks(aa("aaGun", 0, 1, 1), 3), 1);

    assertThat(totalDice(AaFireCap.cappedFiring(firing, 2, BY_DEFENSE))).isEqualTo(2);
  }

  @Test
  void manyOneShotGunsAreCappedAtTheAirTargetCount() {
    final Map<CombatProfile, Integer> firing = Map.of(withMaxAaAttacks(aa("aaGun", 0, 1, 1), 1), 5);

    assertThat(totalDice(AaFireCap.cappedFiring(firing, 2, BY_DEFENSE))).isEqualTo(2);
  }

  @Test
  void noAirTargetsFiresNoDice() {
    final Map<CombatProfile, Integer> firing = Map.of(aa("aaGun", 0, 1, 1), 1);

    assertThat(AaFireCap.cappedFiring(firing, 0, BY_DEFENSE)).isEmpty();
  }

  @Test
  void infiniteGunFillsTheTargetsAFiniteGunLeaves() {
    final Map<CombatProfile, Integer> firing = new LinkedHashMap<>();
    firing.put(withMaxAaAttacks(aa("finiteGun", 0, 2, 1), 2), 1);
    firing.put(aa("infiniteGun", 0, 1, 1), 1);

    final Map<CombatProfile, Integer> rolled = AaFireCap.cappedFiring(firing, 3, BY_DEFENSE);

    assertThat(totalDice(rolled)).isEqualTo(3);
    assertThat(diceForGun(rolled, "finiteGun")).isEqualTo(2);
    assertThat(diceForGun(rolled, "infiniteGun")).isEqualTo(1);
  }

  @Test
  void twoFiniteGunsFireStrongestFirstAndTruncateTheWeaker() {
    final Map<CombatProfile, Integer> firing = new LinkedHashMap<>();
    firing.put(withMaxAaAttacks(aa("strongGun", 0, 2, 1), 3), 1);
    firing.put(withMaxAaAttacks(aa("weakGun", 0, 1, 1), 2), 1);

    final Map<CombatProfile, Integer> rolled = AaFireCap.cappedFiring(firing, 4, BY_DEFENSE);

    assertThat(totalDice(rolled)).isEqualTo(4);
    assertThat(diceForGun(rolled, "strongGun")).isEqualTo(3);
    assertThat(diceForGun(rolled, "weakGun")).isEqualTo(1);
  }

  private static int totalDice(final Map<CombatProfile, Integer> firing) {
    return firing.entrySet().stream().mapToInt(e -> e.getValue() * e.getKey().rolls()).sum();
  }

  private static int diceForGun(final Map<CombatProfile, Integer> firing, final String typeName) {
    return firing.entrySet().stream()
        .filter(e -> e.getKey().type().name().equals(typeName))
        .mapToInt(e -> e.getValue() * e.getKey().rolls())
        .sum();
  }
}
