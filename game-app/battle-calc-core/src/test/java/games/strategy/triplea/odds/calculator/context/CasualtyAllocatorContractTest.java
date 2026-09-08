package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.air;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.multiHp;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
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
      (eligible, stats, side) ->
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
                new Constraints(false, false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.OFFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, fullTank, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, damagedTank, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, damagedTank, Lifecycle.DEAD)).isOne();
  }

  /**
   * Mirrors {@code DummyPlayer#selectCasualties} lines 206-222: keep-one-land spares only the LAST
   * land unit, it does not blanket-protect land. With two land units and a land-preferring order,
   * the first hit takes a land unit (preference wins, it is not the last); the second hit would
   * take the now-last land unit, but keep-one-land redirects it onto an eligible air unit instead.
   * A degenerate "never allocate to land while a non-land unit is eligible" impl kills both air and
   * spares both land, so it produces zero land deaths and fails this.
   */
  @Test
  void keepOneLandSparesTheLastLandUnitByKillingAirInstead() {
    final CombatProfile landUnit = land("infantry", 1, 2, 1);
    final CombatProfile airUnit = air("fighter", 3, 3, 1);
    final Force side =
        new Force(
            Map.of(
                new Key(landUnit, Lifecycle.ACTIVE), 2,
                new Key(airUnit, Lifecycle.ACTIVE), 2));
    final TargetFilter eligible = new TargetFilter(Set.of(landUnit, airUnit));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                2,
                eligible,
                new Dependents(Map.of()),
                new Constraints(true, false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.DEFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, landUnit, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, landUnit, Lifecycle.DEAD)).isEqualTo(1);
    assertThat(countAt(result, airUnit, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, airUnit, Lifecycle.DEAD)).isEqualTo(1);
  }

  /**
   * Design §4: an IMMEDIATE hit that kills a transport removes the cargo it carries in the same
   * allocation — there's no other transport for the infantry to ride, so it goes down now.
   *
   * <p>1a simplification: the model assumes cargo dies with its transport; it omits the engine's
   * rehosting rule (cargo survives if another surviving transport could carry it). Not verified
   * fidelity — a single transport keeps that gap out of this case.
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
                new Constraints(false, false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.OFFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, transport, Lifecycle.DEAD)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isEqualTo(1);
  }

  /**
   * Cargo removal does not wait on firing-mode timing: because cargo never fires, a DEFERRED hit
   * that kills a transport sheds the cargo in the same allocation, exactly as the IMMEDIATE case
   * does. Pins that firing mode is irrelevant to a non-combatant dependent's death.
   *
   * <p>1a simplification: as in the IMMEDIATE case, "cargo dies with its transport" omits the
   * engine's rehosting rule (cargo survives if another surviving transport could carry it) — a
   * known gap, not verified fidelity.
   */
  @Test
  void deferredHitKillingATransportStillRemovesItsCargo() {
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
                new Constraints(false, false),
                PREFERS_LAND,
                FiringMode.DEFERRED,
                Side.OFFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, transport, Lifecycle.DEAD)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isZero();
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isEqualTo(1);
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
                new Constraints(false, false),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.DEFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, infantry, Lifecycle.ACTIVE)).isEqualTo(1);
    assertThat(countAt(result, infantry, Lifecycle.DEAD)).isZero();
  }

  /**
   * The transport restriction as a hard eligibility rule: while a non-transport combatant is
   * eligible the transport is withheld from the preference, and it becomes a legal casualty only
   * once every combatant is dead — one hit kills the destroyer and spares the transport, a second
   * then falls to the now-unprotected transport. Mirrors {@code SelectMainBattleCasualties#apply}'s
   * saturate-then-overflow accounting.
   */
  @Test
  void restrictedTransportIsWithheldUntilEveryCombatantIsDead() {
    final CombatProfile destroyer = sea("destroyer", 3, 3, 1);
    final CombatProfile transport = withFlags(sea("transport", 0, 1, 1), CombatFlag.IS_TRANSPORT);
    final Force side =
        new Force(
            Map.of(
                new Key(destroyer, Lifecycle.ACTIVE), 1,
                new Key(transport, Lifecycle.ACTIVE), 1));
    final TargetFilter eligible = new TargetFilter(Set.of(destroyer, transport));

    final Force oneHit =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                1,
                eligible,
                new Dependents(Map.of()),
                new Constraints(false, true),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.OFFENSE,
                new ProfileStats(Map.of()));
    assertThat(countAt(oneHit, destroyer, Lifecycle.DEAD)).isOne();
    assertThat(countAt(oneHit, transport, Lifecycle.ACTIVE)).isOne();

    final Force twoHits =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                2,
                eligible,
                new Dependents(Map.of()),
                new Constraints(false, true),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.OFFENSE,
                new ProfileStats(Map.of()));
    assertThat(countAt(twoHits, destroyer, Lifecycle.DEAD)).isOne();
    assertThat(countAt(twoHits, transport, Lifecycle.DEAD)).isOne();
  }

  /**
   * Regression pin for the frozen-eligibility bug: the "is a combatant still alive?" test must read
   * the live working map, not the plan-time eligibility filter. A 2-HP battleship damaged by the
   * first hit migrates to a successor absent from the (undamaged-only) filter; reading the filter
   * would see "no combatant left" and sink the protected transport. Reading the live map keeps the
   * damaged battleship counted, so the second hit is dropped — the accepted lone-multi-HP gap — and
   * the transport survives. Survivors must NOT be {@code {battleship-damaged, transport-dead}}.
   */
  @Test
  void restrictedTransportSurvivesWhileADamagedMultiHpCombatantIsStillActive() {
    final CombatProfile battleship = multiHp("battleship", 4, 4, 2, Domain.SEA);
    final CombatProfile damagedBattleship = battleship.onHit().orElseThrow();
    final CombatProfile transport = withFlags(sea("transport", 0, 1, 1), CombatFlag.IS_TRANSPORT);
    final Force side =
        new Force(
            Map.of(
                new Key(battleship, Lifecycle.ACTIVE), 1,
                new Key(transport, Lifecycle.ACTIVE), 1));
    // Eligibility is frozen from undamaged profiles, so the damaged successor is deliberately absent.
    final TargetFilter eligible = new TargetFilter(Set.of(battleship, transport));

    final Force result =
        new ReferenceCasualtyAllocator()
            .allocate(
                side,
                2,
                eligible,
                new Dependents(Map.of()),
                new Constraints(false, true),
                PREFERS_LAND,
                FiringMode.IMMEDIATE,
                Side.OFFENSE,
                new ProfileStats(Map.of()));

    assertThat(countAt(result, transport, Lifecycle.ACTIVE)).isOne();
    assertThat(countAt(result, transport, Lifecycle.DEAD)).isZero();
    assertThat(countAt(result, damagedBattleship, Lifecycle.ACTIVE)).isOne();
  }
}
