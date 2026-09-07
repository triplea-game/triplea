package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.triplea.odds.calculator.adapter.BoundedContextAggregateResults;
import games.strategy.triplea.odds.calculator.adapter.EngineRandomSource;
import games.strategy.triplea.odds.calculator.adapter.GameDataBattleAdapter;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.odds.calculator.context.vector.VectorizedHitRoller;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Setter;

/**
 * The engine-free odds calculator: it holds the live {@link GameData} without cloning or
 * serializing it, bakes a self-contained {@link BattleScenario} per {@link #calculate} via {@link
 * GameDataBattleAdapter}, and fans the run count across worker threads that share that one
 * immutable scenario. Each worker owns its {@link ReferenceBattleSimulator} and dice source, so the
 * only state crossing threads is the read-only scenario.
 */
class BoundedContextBattleCalculator implements IBattleCalculator {
  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  // The live game the adapter reads; never cloned. Volatile because setGameData runs off the caller
  // thread while calculate runs on a worker thread.
  @Nullable private volatile GameData gameData;

  @Setter private volatile boolean keepOneAttackingLandUnit = false;
  @Setter private volatile boolean amphibious = false;
  @Setter private volatile int retreatAfterRound = -1;
  @Setter private volatile int retreatAfterXUnitsLeft = -1;
  @Setter private volatile String attackerOrderOfLosses = null;
  @Setter private volatile String defenderOrderOfLosses = null;
  // Injectable so tests can drive the simulator with a scripted source. Shared across the fan-out
  // workers, so it must be thread-safe when runCount splits (PlainRandomSource is).
  @Setter private volatile IRandomSource randomSource = new PlainRandomSource();
  private volatile boolean cancelled = false;

  @Override
  public CompletableFuture<Boolean> setGameData(@Nullable final GameData data) {
    this.gameData = data;
    return CompletableFuture.completedFuture(data != null);
  }

  @Override
  public void cancel() {
    cancelled = true;
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    final long start = System.currentTimeMillis();
    final GameData data = gameData;
    if (data == null || runCount <= 0) {
      return new ListBackedAggregateResults(0);
    }
    cancelled = false;
    if (amphibious) {
      attacking.forEach(unit -> unit.setWasAmphibious(true));
    }
    final BattleOptions options =
        new BattleOptions(
            retreatWhenOnlyAirLeft,
            orderOfLossTypes(attackerOrderOfLosses, attacking, data),
            orderOfLossTypes(defenderOrderOfLosses, defending, data),
            retreatAfterRound,
            retreatAfterXUnitsLeft,
            keepOneAttackingLandUnit);
    final GameDataBattleAdapter adapter = new GameDataBattleAdapter();
    final BattleScenario scenario =
        adapter.toScenario(
            attacker,
            defender,
            location,
            attacking,
            defending,
            bombarding,
            territoryEffects,
            options);
    final SimulationResults results = simulate(scenario, runCount);
    final AggregateResults aggregateResults =
        new BoundedContextAggregateResults(results, scenario.cost(), attacking, defending);
    aggregateResults.setTime(System.currentTimeMillis() - start);
    return aggregateResults;
  }

  /**
   * Splits {@code runCount} across worker threads that each simulate a chunk over the shared
   * immutable {@code scenario} with their own simulator and dice source, then concatenates the
   * per-run results. Cancellation is polled between chunks — an in-flight chunk finishes.
   */
  private SimulationResults simulate(final BattleScenario scenario, final int runCount) {
    final int workers = Math.max(1, Math.min(MAX_THREADS, runCount));
    final RunCountDistributor distributor = new RunCountDistributor(runCount, workers);
    final List<Integer> chunks = new ArrayList<>(workers);
    for (int i = 0; i < workers; i++) {
      chunks.add(distributor.nextRunCount());
    }
    final List<BattleResult> merged =
        chunks.parallelStream()
            .filter(chunk -> chunk > 0 && !cancelled)
            .flatMap(chunk -> simulateChunk(scenario, chunk).results().stream())
            .collect(Collectors.toList());
    return new SimulationResults(merged);
  }

  private SimulationResults simulateChunk(final BattleScenario scenario, final int chunk) {
    return new ReferenceBattleSimulator(new VectorizedHitRoller())
        .simulate(scenario, chunk, new EngineRandomSource(randomSource));
  }

  /**
   * Collapses a parsed order-of-losses (a casualty-first unit list) into the distinct type sequence
   * {@link BattleOptions} carries; a blank order yields an empty list and the reference default.
   */
  private static List<UnitTypeId> orderOfLossTypes(
      final String ool, final Collection<Unit> units, final GameData data) {
    final List<Unit> ordered = OrderOfLossesInputPanel.getUnitListByOrderOfLoss(ool, units, data);
    if (ordered == null) {
      return List.of();
    }
    return ordered.stream()
        .map(unit -> new UnitTypeId(unit.getType().getName()))
        .distinct()
        .collect(Collectors.toList());
  }
}
