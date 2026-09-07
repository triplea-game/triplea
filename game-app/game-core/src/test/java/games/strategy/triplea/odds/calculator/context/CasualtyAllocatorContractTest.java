package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.multiHp;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceCasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link CasualtyAllocator} (design §4/§6 invariant 5): the allocator, not the
 * {@link CasualtyOrder} it's handed, owns every hard rule — concentration via per-hit {@code
 * onHit()}, keep-one-land, dependent cascade, and target eligibility. Run against the real {@link
 * ReferenceCasualtyAllocator}, which is still a throwing "phase 1" stub, so every test here is RED
 * at runtime by design until that impl lands.
 *
 * <p>{@code allocate} returns the post-hit {@link Force} (immutable value, no in-place mutation),
 * so each case asserts by re-reading the returned force's counts. The dependent-cascade cases pin
 * the {@link FiringMode} timing split from design §4: IMMEDIATE removes cargo in this same
 * allocation; DEFERRED lets the cargo survive this call so it can still fire this round, dying only
 * at a later reconcile the single allocate call does not perform.
 */
class CasualtyAllocatorContractTest {

  /**
   * Order stub used across tests: a pure preference double, standing in for the collaborator pinned
   * separately by {@link OolCasualtyOrderTest}. It always prefers a land profile when one is
   * eligible, otherwise returns whatever's eligible — enough to force the allocator's keep-one-land
   * constraint to actually override the preference rather than coincidentally agree with it.
   */
  private static final CasualtyOrder PREFERS_LAND =
      (eligible, stats) ->
          eligible.stream()
              .filter(p -> p.domain() == Domain.LAND)
              .findFirst()
              .orElseGet(() -> eligible.iterator().next());

  private static int countAt(
      final Force force, final CombatProfile profile, final Lifecycle state) {
    return force.counts().getOrDefault(new Key(profile, state), 0);
  }

  /**
   * Pins invariant 2/3 (design §6): damage is a migration, and multi-hit concentration is per-hit
   * {@code onHit()} feedback, not a separate pending-hits bucket. Two hits landing on a single 2-HP
   * bucket must finish that one unit, not spread half-damage across two.
   */
  @Test
  void twoHitsOnOneTwoHitPointBucketKillOneUnitInsteadOfDamagingTwo() {
    final CombatProfile fullTank = multiHp("tank", 3, 3, 2, Domain.LAND);
    final CombatProfile damagedTank = fullTank.onHit().orElseThrow();
    final Force side = new Force(Map.of(new Key(fullTank, Lifecycle.ACTIVE), 1));
    final TargetFilter eligible = new TargetFilter(Set.of(fullTank, damagedTank));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                2,
                eligible,
                new Dependents(Map.of()),
                new Constraints(false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE);

    assertThat(countAt(result, fullTank, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, damagedTank, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, damagedTank, Lifecycle.DEAD)).isOne();
  }

  /**
   * Mirrors {@code DummyPlayer#selectCasualties} lines 206-222: the allocator must never let the
   * last land unit die while a non-land alternative is eligible, even though the supplied
   * preference ranks land ahead of air. The land unit survives and an air unit dies in its place.
   */
  @Test
  void keepOneLandSparesTheLastLandUnitByKillingAirInstead() {
    final CombatProfile landUnit = land("infantry", 1, 2, 1);
    final CombatProfile airUnit = air("fighter", 3, 3, 1);
    final Force side =
        new Force(
            Map.of(
                new Key(landUnit, Lifecycle.ACTIVE), 1,
                new Key(airUnit, Lifecycle.ACTIVE), 2));
    final TargetFilter eligible = new TargetFilter(Set.of(landUnit, airUnit));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                1,
                eligible,
                new Dependents(Map.of()),
                new Constraints(true),
                PREFERS_LAND,
                FiringMode.IMMEDIATE);

    assertThat(countAt(result, landUnit, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, landUnit, Lifecycle.DEAD)).isZero();
    assertThat(countAt(result, airUnit, Lifecycle.DEAD)).isEqualTo(1);
  }

  /**
   * Design §4: an IMMEDIATE hit that kills a transport removes the cargo it carries in the same
   * allocation — there's no other transport for the infantry to ride, so it goes down now.
   */
  @Test
  void immediateHitKillingATransportRemovesItsCargoInTheSameAllocation() {
    final CombatProfile transport = sea("transport", 0, 1, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force side =
        new Force(
            Map.of(
                new Key(transport, Lifecycle.ACTIVE), 1,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final TargetFilter eligible = new TargetFilter(Set.of(transport));
    final Dependents deps = new Dependents(Map.of(transport, new CargoRule(infantry.type(), 1)));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                1,
                eligible,
                deps,
                new Constraints(false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE);

    assertThat(countAt(result, transport, Lifecycle.DEAD)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isEqualTo(1);
  }

  /**
   * The other half of the design §4 cascade-timing rule: under DEFERRED the cargo of a killed
   * transport still fires this round, so this single allocation leaves it ACTIVE — it dies only at
   * the later reconcile, which one {@code allocate} call does not perform. The transport itself is
   * already dead; only the cargo's removal is deferred.
   */
  @Test
  void deferredHitKillingATransportLeavesItsCargoActiveUntilReconcile() {
    final CombatProfile transport = sea("transport", 0, 1, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force side =
        new Force(
            Map.of(
                new Key(transport, Lifecycle.ACTIVE), 1,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final TargetFilter eligible = new TargetFilter(Set.of(transport));
    final Dependents deps = new Dependents(Map.of(transport, new CargoRule(infantry.type(), 1)));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side, 1, eligible, deps, new Constraints(false), PREFERS_LAND, FiringMode.DEFERRED);

    assertThat(countAt(result, transport, Lifecycle.DEAD)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isZero();
  }

  /**
   * Pins {@link TargetFilter} as a hard rule: hits restricted to air-eligible profiles (the AA
   * shape) must never touch a land unit, no matter how many hits land or how the preference would
   * have ranked it if it were eligible.
   */
  @Test
  void targetFilterRestrictedToAirNeverKillsAnEligibleLandUnit() {
    final CombatProfile fighter = air("fighter", 3, 3, 1);
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final Force side =
        new Force(
            Map.of(
                new Key(fighter, Lifecycle.ACTIVE), 1,
                new Key(infantry, Lifecycle.ACTIVE), 1));
    final TargetFilter aaEligible = new TargetFilter(Set.of(fighter));

    // More hits than the single air unit has hit-points, to prove overflow hits can't spill onto
    // the ineligible land unit either.
    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                3,
                aaEligible,
                new Dependents(Map.of()),
                new Constraints(false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE);

    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isZero();
  }
}
