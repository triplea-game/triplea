package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;

import com.sun.management.ThreadMXBean;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.adapter.EngineRandomSource;
import games.strategy.triplea.odds.calculator.adapter.GameDataBattleAdapter;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.odds.calculator.context.vector.VectorizedHitRoller;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A throwaway profiling spike (design §1, §5): it does not assert, it prints numbers, and it is
 * gated out of {@code check}/{@code ./verify} by {@code @Tag("bench")}. It exists to answer one
 * question before stage-1b piece #2 (the cross-run batched simulator) is built at all: <em>is the
 * per-run map churn / boxing in {@link ReferenceBattleSimulator} the dominant cost?</em> If it is
 * not, primitive-array batching cannot pay off and the batched simulator should not be built.
 *
 * <p>Two measurements per scenario:
 *
 * <ol>
 *   <li><b>The gate</b> — the isolated {@code simulate(...)} loop, reporting ns/run and (via {@link
 *       ThreadMXBean#getThreadAllocatedBytes}) bytes allocated per run. Bytes/run is the direct
 *       proxy for the {@code LinkedHashMap}/{@code Integer}-boxing churn that piece #2 removes.
 *   <li><b>The baseline</b> — end-to-end {@code BattleCalculator.calculate} with the
 *       bounded-context flag off (the shipping {@code MustFightBattle} path) versus on. Confirms
 *       whether stage 1a already banked the dominant clone+replay win, and sets the wall-clock bar
 *       piece #2 must beat.
 * </ol>
 *
 * <p>Numbers are indicative, not a JMH-grade benchmark: single-fork, in-process, one warmed loop.
 * That is deliberate — the decision it feeds (build piece #2 or not) is order-of-magnitude, not a
 * speedup claim. Run with {@code ./gradlew :game-core:benchTest}.
 */
@Tag("bench")
class BattleCalcProfilingBench extends AbstractClientSettingTestCase {

  private static final long SEED = 20260907L;
  private static final int WARMUP_RUNS = 3000;
  private static final int SIM_RUNS = 20_000;
  private static final int E2E_RUNS = 2000;

  private static final ThreadMXBean THREADS = (ThreadMXBean) ManagementFactory.getThreadMXBean();

  @Test
  void profileBoundedContextPath() {
    final List<Case> cases = List.of(smallCase(), mediumCase(), largeCase());

    System.out.println();
    System.out.println("=== battle-calc profiling spike (seed " + SEED + ") ===");
    System.out.println(
        "Gate: isolated ReferenceBattleSimulator(VectorizedHitRoller).simulate over "
            + SIM_RUNS
            + " runs");
    System.out.printf("%-22s %12s %14s%n", "scenario", "ns/run", "bytes/run");
    for (final Case c : cases) {
      profileSimLoop(c);
    }

    System.out.println();
    System.out.println(
        "Baseline: end-to-end BattleCalculator.calculate over " + E2E_RUNS + " runs");
    System.out.printf("%-22s %12s %12s %10s%n", "scenario", "engine ms", "bounded ms", "ratio");
    for (final Case c : cases) {
      profileEndToEnd(c);
    }
    System.out.println();
    System.out.println(
        "Read: a large bytes/run dominated by map/boxing allocation is the precondition for");
    System.out.println(
        "piece #2 paying off; a small bytes/run means int[] batching cannot help and it should not");
    System.out.println("be built. See the writeup in .docs/ for the go/no-go call.");
  }

  /** The gate: allocation and time of the pure simulate loop, adapter cost excluded. */
  private static void profileSimLoop(final Case c) {
    final BattleScenario scenario = c.toScenario();
    final Supplier<EngineRandomSource> rng =
        () -> new EngineRandomSource(new PlainRandomSource(SEED));

    // Warm up the JIT so the timed loop measures steady-state, not interpretation + compilation.
    new ReferenceBattleSimulator(new VectorizedHitRoller())
        .simulate(scenario, WARMUP_RUNS, rng.get());

    final long thread = Thread.currentThread().threadId();
    final long allocBefore = THREADS.getThreadAllocatedBytes(thread);
    final long nanosBefore = System.nanoTime();
    new ReferenceBattleSimulator(new VectorizedHitRoller()).simulate(scenario, SIM_RUNS, rng.get());
    final long nanos = System.nanoTime() - nanosBefore;
    final long alloc = THREADS.getThreadAllocatedBytes(thread) - allocBefore;

    System.out.printf("%-22s %,12d %,14d%n", c.name(), nanos / SIM_RUNS, alloc / SIM_RUNS);
  }

  /** The baseline: engine path vs bounded-context path, same runs, same seed, wall-clock only. */
  private void profileEndToEnd(final Case c) {
    final long engineMs = timeCalculate(c, false);
    final long boundedMs = timeCalculate(c, true);
    System.out.printf(
        "%-22s %,12d %,12d %10.2f%n",
        c.name(), engineMs, boundedMs, engineMs == 0 ? 0.0 : (double) boundedMs / engineMs);
  }

  private long timeCalculate(final Case c, final boolean boundedContext) {
    if (boundedContext) {
      final BoundedContextBattleCalculator calc = new BoundedContextBattleCalculator();
      calc.setGameData(c.gameData());
      calc.setRandomSource(new PlainRandomSource(SEED));
      return timeCalculate(c, calc::calculate);
    }
    final BattleCalculator calc = new BattleCalculator(c.gameData());
    calc.setRandomSource(new PlainRandomSource(SEED));
    return timeCalculate(c, calc::calculate);
  }

  private long timeCalculate(final Case c, final CalculateCall call) {
    final long start = System.nanoTime();
    call.calculate(
        c.attacker(),
        c.defender(),
        c.location(),
        c.attacking(),
        c.defending(),
        List.of(),
        TerritoryEffectHelper.getEffects(c.location()),
        false,
        E2E_RUNS);
    return (System.nanoTime() - start) / 1_000_000;
  }

  /** The shared shape of both calculators' {@code calculate}, so the bench can time either. */
  @FunctionalInterface
  private interface CalculateCall {
    AggregateResults calculate(
        GamePlayer attacker,
        GamePlayer defender,
        Territory location,
        Collection<Unit> attacking,
        Collection<Unit> defending,
        Collection<Unit> bombarding,
        Collection<TerritoryEffect> territoryEffects,
        boolean retreatWhenOnlyAirLeft,
        int runCount);
  }

  private static Case smallCase() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    return new Case(
        "small 5inf v 5inf",
        gameData,
        russians(gameData),
        germans(gameData),
        territory("Germany", gameData),
        infantry(gameData).create(5, russians(gameData)),
        infantry(gameData).create(5, germans(gameData)));
  }

  private static Case mediumCase() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Collection<Unit> attacking =
        new ArrayList<>(infantry(gameData).create(10, russians(gameData)));
    attacking.addAll(armour(gameData).create(4, russians(gameData)));
    attacking.addAll(artillery(gameData).create(2, russians(gameData)));
    attacking.addAll(fighter(gameData).create(2, russians(gameData)));
    final Collection<Unit> defending =
        new ArrayList<>(infantry(gameData).create(8, germans(gameData)));
    defending.addAll(armour(gameData).create(3, germans(gameData)));
    defending.addAll(fighter(gameData).create(2, germans(gameData)));
    return new Case(
        "medium mixed",
        gameData,
        russians(gameData),
        germans(gameData),
        territory("Germany", gameData),
        attacking,
        defending);
  }

  private static Case largeCase() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Collection<Unit> attacking =
        new ArrayList<>(infantry(gameData).create(30, russians(gameData)));
    attacking.addAll(armour(gameData).create(15, russians(gameData)));
    attacking.addAll(artillery(gameData).create(8, russians(gameData)));
    attacking.addAll(fighter(gameData).create(6, russians(gameData)));
    final Collection<Unit> defending =
        new ArrayList<>(infantry(gameData).create(30, germans(gameData)));
    defending.addAll(armour(gameData).create(15, germans(gameData)));
    defending.addAll(artillery(gameData).create(8, germans(gameData)));
    defending.addAll(fighter(gameData).create(6, germans(gameData)));
    return new Case(
        "large mixed stacks",
        gameData,
        russians(gameData),
        germans(gameData),
        territory("Germany", gameData),
        attacking,
        defending);
  }

  /**
   * One profiling scenario: raw {@code calculate} inputs plus a lazily-baked {@link
   * BattleScenario}.
   */
  private record Case(
      String name,
      GameData gameData,
      GamePlayer attacker,
      GamePlayer defender,
      Territory location,
      Collection<Unit> attacking,
      Collection<Unit> defending) {

    BattleScenario toScenario() {
      return new GameDataBattleAdapter()
          .toScenario(
              attacker,
              defender,
              location,
              attacking,
              defending,
              List.of(),
              TerritoryEffectHelper.getEffects(location),
              new BattleOptions(false, List.of(), List.of()));
    }
  }
}
