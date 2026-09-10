package games.strategy.triplea.odds.calculator.context.reference;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.sea;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.withFlags;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.FakeRandomSource;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the round-end unescorted-transport sweep ({@link ReferenceBattleSimulator}) in isolation:
 * the {@code onlyUnescortedTransportsLeft} trigger at the predicate level, and the removal itself
 * end-to-end. The escort defends at 0 so the attacker never takes a hit — this keeps the fixture
 * off the multi-hit-point concentration path so the sweep, not the allocator's hit accounting, is
 * what the assertion sees. Matching the engine ({@code
 * RemoveUnprotectedUnits#checkUndefendedTransports} counts the escorts still waiting to die), the
 * sweep is deferred a round: the transport survives the round its escort dies and is swept the
 * following round, once the raider's fire has missed it. With the restriction off the sweep is a
 * no-op and the attacker simply shoots the transport down the next round instead.
 */
class TransportSweepTest {

  private static final CombatProfile TRANSPORT =
      withFlags(sea("transport", 0, 0, 1), CombatFlag.IS_TRANSPORT);
  private static final CombatProfile DESTROYER = sea("destroyer", 3, 3, 1);
  private static final CombatProfile CARGO =
      withFlags(sea("cargo", 0, 0, 1), CombatFlag.IS_DEPENDENT);

  @Test
  void onlyUnescortedTransportsLeftIsTrueWhenEveryActiveFighterIsATransport() {
    final Map<Key, Integer> working = Map.of(new Key(TRANSPORT, Lifecycle.ACTIVE), 2);
    assertThat(ReferenceBattleSimulator.onlyUnescortedTransportsLeft(working)).isTrue();
  }

  @Test
  void onlyUnescortedTransportsLeftIgnoresDependentCargoAlongsideTheTransport() {
    final Map<Key, Integer> working =
        Map.of(
            new Key(TRANSPORT, Lifecycle.ACTIVE), 1,
            new Key(CARGO, Lifecycle.ACTIVE), 2);
    assertThat(ReferenceBattleSimulator.onlyUnescortedTransportsLeft(working)).isTrue();
  }

  @Test
  void onlyUnescortedTransportsLeftIsFalseWhenANonTransportCombatantSurvives() {
    final Map<Key, Integer> working =
        Map.of(
            new Key(TRANSPORT, Lifecycle.ACTIVE), 1,
            new Key(DESTROYER, Lifecycle.ACTIVE), 1);
    assertThat(ReferenceBattleSimulator.onlyUnescortedTransportsLeft(working)).isFalse();
  }

  @Test
  void onlyUnescortedTransportsLeftIsFalseWhenNoTransportRemains() {
    final Map<Key, Integer> working = Map.of(new Key(DESTROYER, Lifecycle.ACTIVE), 1);
    assertThat(ReferenceBattleSimulator.onlyUnescortedTransportsLeft(working)).isFalse();
  }

  /**
   * A raider clears the escort in round one; the now-unescorted transport is not swept that round
   * (the escort is still waiting to die) but survives into round two, and once the raider's round-
   * two shot misses it the restriction sweeps it off while the raider can still fire — a two-round
   * attacker win. Only the first die — the raider's opening shot, fired before any defender — lands;
   * every later roll misses (the 0-defence escort and transport still draw dice that cannot hit), so
   * the raider kills the escort in round one and misses the transport in round two, leaving the
   * sweep, not fire, to remove it.
   */
  @Test
  void restrictionSweepsAnUnescortedTransportTheRoundAfterItsEscortDies() {
    final BattleResult result =
        runRaiderVersusEscortedTransport(
            FakeRandomSource.scripted(0, 5, 5, 5, 5, 5), restricted(), new Dependents(Map.of()));

    assertThat(result.outcome()).isEqualTo(Outcome.ATTACKER_WINS);
    assertThat(result.roundsFought()).isEqualTo(2);
    assertThat(total(result.defenderSurvivors())).isZero();
    assertThat(total(result.attackerSurvivors())).isEqualTo(1);
  }

  /**
   * The same fight with the restriction off: the sweep never fires, so the escort dies in round one
   * and the transport only falls to firing in round two — the removal step is a no-op.
   */
  @Test
  void withoutTheRestrictionTheTransportIsNotSweptAndFallsToFiringNextRound() {
    final BattleResult result =
        runRaiderVersusEscortedTransport(
            FakeRandomSource.alwaysHits(), RulesProfile.standard(), new Dependents(Map.of()));

    assertThat(result.outcome()).isEqualTo(Outcome.ATTACKER_WINS);
    assertThat(result.roundsFought()).isEqualTo(2);
    assertThat(total(result.defenderSurvivors())).isZero();
  }

  /**
   * The sweep path sheds cargo exactly as the firing path does: a swept transport carrying two cargo
   * takes them down with it, so nothing dependent is left as a phantom survivor. The raider misses
   * its round-two shot so the transport falls to the sweep, not to fire, exercising the sweep's own
   * cargo cascade.
   */
  @Test
  void sweptTransportCascadesItsCargo() {
    final Dependents deps =
        new Dependents(Map.of(TRANSPORT, new CargoRule(new UnitTypeId("cargo"), 2)));
    final BattleResult result =
        runRaiderVersusEscortedTransport(
            FakeRandomSource.scripted(0, 5, 5, 5, 5, 5), restricted(), deps, CARGO);

    assertThat(result.roundsFought()).isEqualTo(2);
    assertThat(total(result.defenderSurvivors())).isZero();
    assertThat(result.defenderSurvivors().counts())
        .doesNotContainKey(new Key(CARGO, Lifecycle.ACTIVE));
  }

  private static BattleResult runRaiderVersusEscortedTransport(
      final FakeRandomSource rng,
      final RulesProfile rules,
      final Dependents deps,
      final CombatProfile... extraDefenders) {
    final CombatProfile raider = sea("raider", 3, 3, 1);
    // Escort defends at 0, so it fires nothing and the raider survives untouched; it is still a
    // non-transport combatant that must clear before the transport is unescorted.
    final CombatProfile escort = sea("escort", 3, 0, 1);
    final LinkedHashMap<Key, Integer> defenders = new LinkedHashMap<>();
    defenders.put(new Key(escort, Lifecycle.ACTIVE), 1);
    defenders.put(new Key(TRANSPORT, Lifecycle.ACTIVE), 1);
    for (final CombatProfile extra : extraDefenders) {
      defenders.merge(new Key(extra, Lifecycle.ACTIVE), 2, Integer::sum);
    }
    final Force attackers = new Force(Map.of(new Key(raider, Lifecycle.ACTIVE), 1));
    final BattleScenario scenario =
        new BattleScenario(
            attackers,
            new Force(Map.copyOf(defenders)),
            new Force(Map.of()),
            deps,
            rules,
            List.of(),
            Map.of(),
            false,
            6,
            false,
            new ReferenceRetreatPolicy(-1, -1, false),
            new ReferenceRetreatPolicy(-1, -1, false),
            new OolCasualtyOrder(List.of()),
            new OolCasualtyOrder(List.of()));
    return new ReferenceBattleSimulator().simulate(scenario, 1, rng).results().get(0);
  }

  private static RulesProfile restricted() {
    return new RulesProfile(false, false, true, false, false, false);
  }

  private static int total(final Force force) {
    return force.counts().values().stream().mapToInt(Integer::intValue).sum();
  }
}
