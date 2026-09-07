package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.aa;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.firstStrikeSea;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceRollGroupResolver;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link RollGroupResolver}, pinned against the {@link
 * ReferenceRollGroupResolver} impl — the hardest seam in the bounded context (design §4, §8 stage-2
 * hoist target). All four tests are RED: the reference impl still throws {@code
 * UnsupportedOperationException("phase 1")}.
 *
 * <p>Whether an enemy destroyer negates sub first strike is relational, so it comes through an
 * injected {@link CombatRelations} fake, not from matching unit names — the "destroyer gone
 * restores first strike" case flips the fake's answer between two {@code plan} calls.
 */
class ReferenceRollGroupResolverTest {

  private static Force forceOf(final CombatProfile profile, final int count) {
    return new Force(Map.of(new Key(profile, Lifecycle.ACTIVE), count));
  }

  private static Force forceOf(
      final CombatProfile first,
      final int firstCount,
      final CombatProfile second,
      final int secondCount) {
    return new Force(
        Map.of(
            new Key(first, Lifecycle.ACTIVE), firstCount,
            new Key(second, Lifecycle.ACTIVE), secondCount));
  }

  /**
   * Base-counts pass-through, standing in for a real support evaluation in tests that don't care
   * what it computes.
   */
  private static Map<CombatProfile, Integer> rawCounts(final Force side) {
    final Map<CombatProfile, Integer> counts = new LinkedHashMap<>();
    side.counts().forEach((key, count) -> counts.put(key.profile(), count));
    return counts;
  }

  private static RollGroup groupFiring(final BattleRound round, final CombatProfile profile) {
    return round.firing().sequencedKeySet().stream()
        .filter(group -> group.firing().containsKey(profile))
        .findFirst()
        .orElseThrow(
            () -> new NoSuchElementException("no roll group fires " + profile.type().name()));
  }

  /**
   * Design §4/§10: "one list = one true sequence (AA -> first strike -> main -> ...)" — the
   * resolver must partition one side's units into three groups and order the {@link BattleRound}
   * firing sequence AA first, first strike second, main third, regardless of insertion/Force-map
   * order.
   *
   * <pre>
   * (1) attackers = AA gun + first-strike sub + plain infantry, one Force
   * (2) defenders = plain infantry only; the fake reports first strike NOT negated
   * (3) plan one round
   * (4) validate: firing() key order is [AA group, first-strike group, main group]
   * </pre>
   */
  @Test
  void firingSequenceOrdersAaBeforeFirstStrikeBeforeMainCombat() {
    final CombatProfile aaGun = aa("aaGun", 0, 1, 1);
    final CombatProfile sub = firstStrikeSea("sub", 2, 1, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);

    final Force attackers =
        new Force(
            Map.of(
                new Key(aaGun, Lifecycle.ACTIVE), 1,
                new Key(sub, Lifecycle.ACTIVE), 2,
                new Key(infantry, Lifecycle.ACTIVE), 3));
    final Force defenders = forceOf(land("defendingInfantry", 1, 2, 1), 2);

    final RollGroupResolver resolver =
        new ReferenceRollGroupResolver(
            (force, enemy, side, rules, round) -> rawCounts(force), new FakeCombatRelations(false));

    final BattleRound plan =
        resolver.plan(attackers, defenders, new RulesProfile(Map.of()), List.of(), 1);

    final List<RollGroup> sequence = plan.firing().sequencedKeySet().stream().toList();
    assertThat(sequence).hasSize(3);
    assertThat(sequence.get(0).firing()).containsKey(aaGun);
    assertThat(sequence.get(1).firing()).containsKey(sub);
    assertThat(sequence.get(2).firing()).containsKey(infantry);
  }

  /**
   * Design §4 ("Re-run each round because composition changes the plan — a dead destroyer restores
   * sub first strike") and §5 step 1: the resolver is pure per-round, so calling {@code plan} twice
   * must recompute the sub's first-strike eligibility from the current {@link CombatRelations}
   * answer, not cache it.
   *
   * <pre>
   * (1) round A: fake reports first strike negated (blocking destroyer) -> subs fire DEFERRED
   * (2) round B: fake flips to not-negated (destroyer gone) -> a FRESH plan() restores IMMEDIATE
   * </pre>
   */
  @Test
  void freshPlanCallRestoresSubFirstStrikeOnceTheEnemyDestroyerIsGone() {
    final CombatProfile sub = firstStrikeSea("sub", 2, 1, 1);
    final CombatProfile destroyer = sea("destroyer", 2, 2, 1);
    final CombatProfile defendingInfantry = land("infantry", 1, 2, 1);
    final Force attackers = forceOf(sub, 2);
    final RulesProfile rules = new RulesProfile(Map.of());
    final FakeCombatRelations relations = new FakeCombatRelations(true);
    final RollGroupResolver resolver =
        new ReferenceRollGroupResolver((force, enemy, side, r, round) -> Map.of(), relations);

    final Force defendersWithDestroyer = forceOf(destroyer, 1, defendingInfantry, 1);
    final BattleRound roundWithDestroyer =
        resolver.plan(attackers, defendersWithDestroyer, rules, List.of(), 1);
    assertThat(groupFiring(roundWithDestroyer, sub).firingMode()).isEqualTo(FiringMode.DEFERRED);

    relations.firstStrikeNegated = false;
    final Force defendersWithoutDestroyer = forceOf(defendingInfantry, 1);
    final BattleRound roundWithoutDestroyer =
        resolver.plan(attackers, defendersWithoutDestroyer, rules, List.of(), 2);
    assertThat(groupFiring(roundWithoutDestroyer, sub).firingMode())
        .isEqualTo(FiringMode.IMMEDIATE);
  }

  /**
   * Design §3.1/§5 step 3a: {@code firingMode} decides which counts feed the roll — first-strike
   * (and, symmetrically, sub) groups fire {@code IMMEDIATE} off the live working map; ordinary
   * main-combat groups fire {@code DEFERRED} off the exchange-start snapshot.
   *
   * <pre>
   * (1) attackers = first-strike sub + plain infantry; the fake reports first strike NOT negated
   * (2) plan one round
   * (3) validate: the sub's group is IMMEDIATE, the infantry's main-combat group is DEFERRED
   * </pre>
   */
  @Test
  void firstStrikeGroupsAreImmediateAndMainCombatGroupsAreDeferred() {
    final CombatProfile sub = firstStrikeSea("sub", 2, 1, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force attackers = forceOf(sub, 2, infantry, 3);
    final Force defenders = forceOf(land("defendingInfantry", 1, 2, 1), 2);
    final RollGroupResolver resolver =
        new ReferenceRollGroupResolver(
            (force, enemy, side, rules, round) -> Map.of(), new FakeCombatRelations(false));

    final BattleRound plan =
        resolver.plan(attackers, defenders, new RulesProfile(Map.of()), List.of(), 1);

    assertThat(groupFiring(plan, sub).firingMode()).isEqualTo(FiringMode.IMMEDIATE);
    assertThat(groupFiring(plan, infantry).firingMode()).isEqualTo(FiringMode.DEFERRED);
  }

  /**
   * Collaboration test for the {@code SupportResolver} seam (design §4: "base counts -> per-round
   * evaluated strengths"). A fake resolver that ignores its inputs and always returns one fixed
   * evaluated map proves the resolver builds {@link RollGroup#firing()} from whatever {@code
   * SupportResolver} answers, not from the raw {@link Force} counts it was given (3, here) —
   * pinning that the seam is actually wired, not hardcoded.
   */
  @Test
  void mainCombatGroupFiringComesFromTheInjectedSupportResolverNotRawForceCounts() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    // Stands in for a +1 support bonus baked into the evaluated attack stat.
    final CombatProfile evaluatedInfantry = land("infantry", 2, 2, 1);
    final Force attackers = forceOf(infantry, 3);
    final Force defenders = forceOf(land("defendingInfantry", 1, 2, 1), 2);
    final Map<CombatProfile, Integer> fakeEvaluation = Map.of(evaluatedInfantry, 77);

    final RollGroupResolver resolver =
        new ReferenceRollGroupResolver(
            (force, enemy, side, rules, round) -> fakeEvaluation, new FakeCombatRelations(false));

    final BattleRound plan =
        resolver.plan(attackers, defenders, new RulesProfile(Map.of()), List.of(), 1);

    final boolean anyGroupUsesTheFakeEvaluation =
        plan.firing().sequencedKeySet().stream()
            .anyMatch(group -> group.firing().equals(fakeEvaluation));
    assertThat(anyGroupUsesTheFakeEvaluation).isTrue();
  }

  /**
   * Collaboration test for the {@code CombatRelations} targeting seam (design §4: targeting reads
   * the opposing composition): the resolver must set each {@link RollGroup#target()} from {@link
   * CombatRelations#eligibleTargets}, not leave it empty. A fake returning one fixed filter proves
   * every planned group carries the seam's answer rather than a hardcoded empty filter.
   */
  @Test
  void eachGroupTargetIsPopulatedFromCombatRelationsEligibleTargets() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile defendingInfantry = land("defendingInfantry", 1, 2, 1);
    final Force attackers = forceOf(infantry, 3);
    final Force defenders = forceOf(defendingInfantry, 2);
    final TargetFilter onlyDefendingInfantry = new TargetFilter(Set.of(defendingInfantry));
    final FakeCombatRelations relations = new FakeCombatRelations(false);
    relations.targets = onlyDefendingInfantry;
    final RollGroupResolver resolver =
        new ReferenceRollGroupResolver(
            (force, enemy, side, rules, round) -> rawCounts(force), relations);

    final BattleRound plan =
        resolver.plan(attackers, defenders, new RulesProfile(Map.of()), List.of(), 1);

    assertThat(plan.firing().sequencedKeySet()).isNotEmpty();
    assertThat(plan.firing().sequencedKeySet())
        .allMatch(group -> group.target().equals(onlyDefendingInfantry));
  }

  /**
   * In-test {@link CombatRelations} whose {@code firstStrikeNegated} answer and {@code
   * eligibleTargets} filter the test controls directly; the submerge method is outside these tests'
   * intent and rejects calls.
   */
  private static final class FakeCombatRelations implements CombatRelations {
    private boolean firstStrikeNegated;
    private TargetFilter targets = new TargetFilter(Set.of());

    FakeCombatRelations(final boolean firstStrikeNegated) {
      this.firstStrikeNegated = firstStrikeNegated;
    }

    @Override
    public boolean firstStrikeNegated(final Side side, final Force friendly, final Force enemy) {
      return firstStrikeNegated;
    }

    @Override
    public TargetFilter eligibleTargets(
        final RollGroup group, final Force friendly, final Force enemy) {
      return targets;
    }

    @Override
    public boolean canSubmerge(final Map<CombatProfile, Integer> cohort, final Force enemy) {
      throw new UnsupportedOperationException("not exercised by this test");
    }
  }
}
