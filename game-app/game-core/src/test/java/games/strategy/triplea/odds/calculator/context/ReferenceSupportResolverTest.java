package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.gives;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.receives;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceSupportResolver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the Support Rule (design §2/§4) against {@link ReferenceSupportResolver}: capacity-limited
 * allocation honoring {@code usesPerGiver}, {@code firstRoundOnly} expiry, and {@code Side}
 * isolation between the offense and defense forces. RED: the reference resolver is still a throwing
 * {@code "phase 1"} stub, so every case here fails on the call, not the assertion.
 */
class ReferenceSupportResolverTest {

  private static final SupportCategory ARTILLERY_GIVES = new SupportCategory("gives:artillery");
  private static final SupportCategory ARTILLERY_RECEIVES =
      new SupportCategory("receives:artillery");

  /**
   * Two artillery x {@code usesPerGiver=2} = 4 uses of support against 5 infantry: capacity falls
   * short of demand, so the split is visible rather than trivially "everyone boosted."
   *
   * <pre>
   * (1) 2 artillery (givers) + 5 infantry (receivers), one round-1 resolve call
   * (2) capacity = 2 givers x 2 uses = 4, less than the 5 receivers
   * (3) validate: 4 infantry evaluated at base+bonus, the remaining 1 stays at base
   * </pre>
   */
  @Test
  void resolveBoostsOnlyAsManyRecipientsAsGiverCapacityAllows() {
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), ARTILLERY_GIVES);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), ARTILLERY_RECEIVES);
    final Force attackers =
        new Force(
            Map.of(
                new Key(artillery, Lifecycle.ACTIVE), 2,
                new Key(infantry, Lifecycle.ACTIVE), 5));
    final SupportRule rule =
        new SupportRule(ARTILLERY_GIVES, ARTILLERY_RECEIVES, 1, true, 2, Side.OFFENSE, false);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(attackers, emptyForce(), Side.OFFENSE, List.of(rule), 1);

    assertThat(evaluated)
        .isEqualTo(Map.of(artillery, 2, boostedAttack(infantry, 1), 4, infantry, 1));
  }

  /**
   * A {@code firstRoundOnly} rule grants its bonus at round 1 and is entirely gone by round 2 — the
   * same force, resolved twice, must produce two different evaluated maps.
   */
  @Test
  void firstRoundOnlySupportExpiresAfterRoundOne() {
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), ARTILLERY_GIVES);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), ARTILLERY_RECEIVES);
    final Force attackers =
        new Force(
            Map.of(
                new Key(artillery, Lifecycle.ACTIVE), 1,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final SupportRule rule =
        new SupportRule(ARTILLERY_GIVES, ARTILLERY_RECEIVES, 1, true, 1, Side.OFFENSE, true);
    final List<SupportRule> rules = List.of(rule);

    final Map<CombatProfile, Integer> round1 =
        new ReferenceSupportResolver().resolve(attackers, emptyForce(), Side.OFFENSE, rules, 1);
    final Map<CombatProfile, Integer> round2 =
        new ReferenceSupportResolver().resolve(attackers, emptyForce(), Side.OFFENSE, rules, 2);

    assertThat(round1).isEqualTo(Map.of(artillery, 1, boostedAttack(infantry, 1), 1));
    assertThat(round2).isEqualTo(Map.of(artillery, 1, infantry, 1));
  }

  /**
   * A {@code Side.OFFENSE} rule is scoped to the attacking force for the whole battle (design §2: a
   * rule's {@code side} is a fixed property of the rule, not derived per round) and must never leak
   * into a resolve call made for the defending force, even when that force holds the identical
   * giver/receiver pair and would otherwise qualify.
   */
  @Test
  void offenseSideSupportDoesNotAlterDefendersEvaluatedStrengths() {
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), ARTILLERY_GIVES);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), ARTILLERY_RECEIVES);
    final Force defenders =
        new Force(
            Map.of(
                new Key(artillery, Lifecycle.ACTIVE), 1,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final SupportRule offenseOnlyRule =
        new SupportRule(ARTILLERY_GIVES, ARTILLERY_RECEIVES, 1, true, 1, Side.OFFENSE, false);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(defenders, emptyForce(), Side.DEFENSE, List.of(offenseOnlyRule), 1);

    assertThat(evaluated).isEqualTo(Map.of(artillery, 1, infantry, 1));
  }

  private static Force emptyForce() {
    return new Force(Map.of());
  }

  /** The evaluated profile a support bonus produces: {@code base} with {@code bonus} on attack. */
  private static CombatProfile boostedAttack(final CombatProfile base, final int bonus) {
    return new CombatProfile(
        base.type(),
        base.attack() + bonus,
        base.defense(),
        base.rolls(),
        base.hitPoints(),
        base.domain(),
        base.damage(),
        base.gives(),
        base.receives(),
        base.flags(),
        base.next());
  }
}
