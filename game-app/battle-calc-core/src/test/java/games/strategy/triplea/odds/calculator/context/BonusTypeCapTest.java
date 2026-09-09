package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.BonusTypeId;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceSupportResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pins the per-bonusType stacking cap against the engine's {@code
 * AvailableSupports.giveSupportToUnit}: a recipient takes at most {@code maxPerReceiver} supports
 * per bonus type across the rules sharing it, each from a distinct supporter, with strength and
 * roll support allocated from separate pools.
 */
class BonusTypeCapTest {

  private static final SupportCategory GIVES_A = new SupportCategory("gives:a");
  private static final SupportCategory GIVES_B = new SupportCategory("gives:b");
  private static final SupportCategory RECEIVES_A = new SupportCategory("receives:a");
  private static final SupportCategory RECEIVES_B = new SupportCategory("receives:b");

  /**
   * Two distinct artillery each supply one support to the single infantry, stacking to the bonus
   * type's count of 2: the recipient evaluates at base attack + 2 x bonus.
   */
  @Test
  void twoDistinctSupportersStackUpToTheBonusTypeCap() {
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), GIVES_A);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), RECEIVES_A);
    final Force force = force(Map.of(artillery, 2, infantry, 1));
    final SupportRule rule = strengthRule(GIVES_A, RECEIVES_A, 1, 1, "art", 2);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver().resolve(force, empty(), Side.OFFENSE, List.of(rule), 1);

    assertThat(evaluated).isEqualTo(Map.of(artillery, 2, withAttack(infantry, 3), 1));
  }

  /**
   * One artillery with two support uses still gives each recipient only one support — stacking on a
   * single recipient needs distinct supporters — so its two uses spread across two infantry, one
   * each, rather than doubling up on either.
   */
  @Test
  void oneSupporterGivesAtMostOneSupportPerRecipientDespiteCap() {
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), GIVES_A);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), RECEIVES_A);
    final Force force = force(Map.of(artillery, 1, infantry, 2));
    final SupportRule rule = strengthRule(GIVES_A, RECEIVES_A, 1, 2, "art", 2);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver().resolve(force, empty(), Side.OFFENSE, List.of(rule), 1);

    assertThat(evaluated).isEqualTo(Map.of(artillery, 1, withAttack(infantry, 2), 2));
  }

  /**
   * Two rules that grant different categories but share a bonus type compete under one cap: with a
   * count of 1 the recipient keeps only the first rule's support, not one from each.
   */
  @Test
  void sharedBonusTypeCapsSupportAcrossRules() {
    final CombatProfile giverA = gives(land("giverA", 2, 2, 1), GIVES_A);
    final CombatProfile giverB = gives(land("giverB", 2, 2, 1), GIVES_B);
    final CombatProfile infantry = receivesAll(land("infantry", 1, 2, 1), RECEIVES_A, RECEIVES_B);
    final Force force = force(Map.of(giverA, 1, giverB, 1, infantry, 1));
    final SupportRule ruleA = strengthRule(GIVES_A, RECEIVES_A, 1, 1, "shared", 1);
    final SupportRule ruleB = strengthRule(GIVES_B, RECEIVES_B, 1, 1, "shared", 1);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(force, empty(), Side.OFFENSE, List.of(ruleA, ruleB), 1);

    assertThat(evaluated).isEqualTo(Map.of(giverA, 1, giverB, 1, withAttack(infantry, 2), 1));
  }

  /** The same two rules under distinct bonus types each apply, so the recipient stacks both. */
  @Test
  void distinctBonusTypesStackIndependently() {
    final CombatProfile giverA = gives(land("giverA", 2, 2, 1), GIVES_A);
    final CombatProfile giverB = gives(land("giverB", 2, 2, 1), GIVES_B);
    final CombatProfile infantry = receivesAll(land("infantry", 1, 2, 1), RECEIVES_A, RECEIVES_B);
    final Force force = force(Map.of(giverA, 1, giverB, 1, infantry, 1));
    final SupportRule ruleA = strengthRule(GIVES_A, RECEIVES_A, 1, 1, "typeA", 1);
    final SupportRule ruleB = strengthRule(GIVES_B, RECEIVES_B, 1, 1, "typeB", 1);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(force, empty(), Side.OFFENSE, List.of(ruleA, ruleB), 1);

    assertThat(evaluated).isEqualTo(Map.of(giverA, 1, giverB, 1, withAttack(infantry, 3), 1));
  }

  /**
   * Strength and roll support draw from independent pools, so a shared bonus-type name does not
   * make them compete: the recipient gains both a strength point and a roll.
   */
  @Test
  void strengthAndRollSupportAllocateIndependently() {
    final CombatProfile strengthGiver = gives(land("strengthGiver", 2, 2, 1), GIVES_A);
    final CombatProfile rollGiver = gives(land("rollGiver", 2, 2, 1), GIVES_B);
    final CombatProfile infantry = receivesAll(land("infantry", 1, 2, 1), RECEIVES_A, RECEIVES_B);
    final Force force = force(Map.of(strengthGiver, 1, rollGiver, 1, infantry, 1));
    final SupportRule strengthRule = strengthRule(GIVES_A, RECEIVES_A, 1, 1, "shared", 1);
    final SupportRule rollRule =
        new SupportRule(
            GIVES_B, RECEIVES_B, 1, false, 1, Side.OFFENSE, false, new BonusTypeId("shared"), 1, 1);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(force, empty(), Side.OFFENSE, List.of(strengthRule, rollRule), 1);

    final CombatProfile boosted = withRolls(withAttack(infantry, 2), 2);
    assertThat(evaluated).isEqualTo(Map.of(strengthGiver, 1, rollGiver, 1, boosted, 1));
  }

  /**
   * Two rules share a bonus type with count 1 but grant different bonuses. The engine's
   * SupportRuleSort applies the stronger rule first, so a capped receiver keeps the +2 rather than
   * the +1 — and it must not depend on the order the rules happen to be supplied in.
   */
  @Test
  void higherBonusRuleWinsTheSharedCapRegardlessOfInputOrder() {
    final CombatProfile weakGiver = gives(land("weakGiver", 2, 2, 1), GIVES_A);
    final CombatProfile strongGiver = gives(land("strongGiver", 2, 2, 1), GIVES_B);
    final CombatProfile infantry = receivesAll(land("infantry", 1, 2, 1), RECEIVES_A, RECEIVES_B);
    final Force force = force(Map.of(weakGiver, 1, strongGiver, 1, infantry, 1));
    final SupportRule weak = strengthRule(GIVES_A, RECEIVES_A, 1, 1, "shared", 1);
    final SupportRule strong = strengthRule(GIVES_B, RECEIVES_B, 2, 1, "shared", 1);

    // Supplied weak-first, yet the +2 rule must win the single-support cap.
    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(force, empty(), Side.OFFENSE, List.of(weak, strong), 1);

    assertThat(evaluated)
        .isEqualTo(Map.of(weakGiver, 1, strongGiver, 1, withAttack(infantry, 3), 1));
  }

  /**
   * Two rules share a bonus type and bonus but target different unit-type breadths. The narrowly
   * targeted rule applies first so its support is not wasted on a unit the broad rule also covers:
   * the marine (targeted by both) takes the narrow support and the infantry still gets the broad
   * one, so both are boosted instead of the infantry going without.
   */
  @Test
  void narrowerRuleAppliesFirstSoNoSupportIsWasted() {
    final CombatProfile broad = gives(land("broad", 2, 2, 1), GIVES_A);
    final CombatProfile narrow = gives(land("narrow", 2, 2, 1), GIVES_B);
    final CombatProfile marine = receivesAll(land("marine", 2, 2, 1), RECEIVES_A, RECEIVES_B);
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), RECEIVES_A);
    final Force force = force(Map.of(broad, 1, narrow, 1, marine, 1, infantry, 1));
    final SupportRule broadRule = sharedRule(GIVES_A, RECEIVES_A, 2);
    final SupportRule narrowRule = sharedRule(GIVES_B, RECEIVES_B, 1);

    final Map<CombatProfile, Integer> evaluated =
        new ReferenceSupportResolver()
            .resolve(force, empty(), Side.OFFENSE, List.of(broadRule, narrowRule), 1);

    assertThat(evaluated)
        .isEqualTo(
            Map.of(broad, 1, narrow, 1, withAttack(marine, 3), 1, withAttack(infantry, 2), 1));
  }

  /** A strength rule in bonus type "shared" with count 1 and the given target-type breadth. */
  private static SupportRule sharedRule(
      final SupportCategory from, final SupportCategory to, final int targetTypeCount) {
    return new SupportRule(
        from, to, 1, true, 1, Side.OFFENSE, false, new BonusTypeId("shared"), 1, targetTypeCount);
  }

  private static SupportRule strengthRule(
      final SupportCategory from,
      final SupportCategory to,
      final int bonus,
      final int usesPerGiver,
      final String bonusType,
      final int maxPerReceiver) {
    return new SupportRule(
        from,
        to,
        bonus,
        true,
        usesPerGiver,
        Side.OFFENSE,
        false,
        new BonusTypeId(bonusType),
        maxPerReceiver,
        1);
  }

  private static Force force(final Map<CombatProfile, Integer> counts) {
    final Map<Key, Integer> keys = new LinkedHashMap<>();
    counts.forEach((profile, count) -> keys.put(new Key(profile, Lifecycle.ACTIVE), count));
    return new Force(keys);
  }

  private static Force empty() {
    return new Force(Map.of());
  }

  private static CombatProfile gives(final CombatProfile base, final SupportCategory category) {
    return CombatProfileFixtures.gives(base, category);
  }

  private static CombatProfile receives(final CombatProfile base, final SupportCategory category) {
    return CombatProfileFixtures.receives(base, category);
  }

  private static CombatProfile receivesAll(
      final CombatProfile base, final SupportCategory... categories) {
    return rebuild(base, base.gives(), Set.of(categories), base.attack(), base.rolls());
  }

  private static CombatProfile withAttack(final CombatProfile base, final int attack) {
    return rebuild(base, base.gives(), base.receives(), attack, base.rolls());
  }

  private static CombatProfile withRolls(final CombatProfile base, final int rolls) {
    return rebuild(base, base.gives(), base.receives(), base.attack(), rolls);
  }

  private static CombatProfile rebuild(
      final CombatProfile base,
      final Set<SupportCategory> gives,
      final Set<SupportCategory> receives,
      final int attack,
      final int rolls) {
    return new CombatProfile(
        base.type(),
        attack,
        base.defense(),
        rolls,
        base.maxRoundsAa(),
        base.maxAaAttacks(),
        base.hitPoints(),
        base.domain(),
        base.damage(),
        gives,
        receives,
        base.flags(),
        base.next());
  }
}
