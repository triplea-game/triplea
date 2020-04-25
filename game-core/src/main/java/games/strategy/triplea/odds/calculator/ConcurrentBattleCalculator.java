package games.strategy.triplea.odds.calculator;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Runnables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.concurrent.ThreadSafe;
import org.triplea.io.IoUtils;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up
 * the run count across these workers. This is mainly to be used by AIs since they call the
 * OddsCalculator a lot.
 */
@ThreadSafe
public class ConcurrentBattleCalculator implements IBattleCalculator {
  private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

  private final ExecutorService executor;
  private final Set<BattleCalculator> calculators = ConcurrentHashMap.newKeySet(MAX_THREADS);
  // shortcut everything if we are shutting down
  private volatile boolean isShutDown = false;

  /**
   * Internal lock to synchronize on in order to prevent modification during initialization of
   * delegate single-threaded {@link BattleCalculator} instances.
   */
  private final Object mutex = new Object();

  private final Runnable dataLoadedAction;
  private byte[] bytes = new byte[0];
  private boolean keepOneAttackingLandUnit = false;
  private boolean amphibious = false;
  private int retreatAfterRound = -1;
  private int retreatAfterXUnitsLeft = -1;
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;

  public ConcurrentBattleCalculator(final String threadNamePrefix) {
    this(threadNamePrefix, Runnables.doNothing());
  }

  ConcurrentBattleCalculator(final String threadNamePrefix, final Runnable dataLoadedAction) {
    executor =
        Executors.newFixedThreadPool(
            MAX_THREADS,
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(threadNamePrefix + " ConcurrentBattleCalculator Worker-%d")
                .build());
    this.dataLoadedAction = dataLoadedAction;
  }

  public void setGameData(final GameData data) {
    synchronized (mutex) {
      bytes = data == null ? new byte[0] : GameDataUtils.serializeGameDataWithoutHistory(data);
      dataLoadedAction.run();
    }
  }

  @Override
  public int getThreadCount() {
    return MAX_THREADS;
  }

  public void shutdown() {
    isShutDown = true;
    cancel();
    executor.shutdown();
  }

  /**
   * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the
   * results. Then waits for all the future results and combines them together.
   */
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
      final int runCount)
      throws IllegalStateException {
    Preconditions.checkState(!isShutDown, "ConcurrentBattleCalculator is already shut down");
    Preconditions.checkState(bytes.length != 0, "Data has not been set yet.");
    final long start = System.currentTimeMillis();
    final int runsPerWorker = runCount / MAX_THREADS;
    final List<Future<AggregateResults>> results;
    synchronized (mutex) {
      results =
          IntStream.range(0, MAX_THREADS)
              .map(index -> index == 0 ? runCount % MAX_THREADS : 0)
              .map(runs -> runs + runsPerWorker)
              .mapToObj(
                  individualRemaining ->
                      createBattleCalcWorker(
                          attacker,
                          defender,
                          location,
                          attacking,
                          defending,
                          bombarding,
                          territoryEffects,
                          retreatWhenOnlyAirLeft,
                          individualRemaining))
              .collect(Collectors.toList());
    }
    final AggregateResults result = aggregateResults(results, runsPerWorker);
    result.setTime(System.currentTimeMillis() - start);
    return result;
  }

  private Future<AggregateResults> createBattleCalcWorker(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runs) {
    final BattleCalculator calculator = new BattleCalculator();
    calculator.setKeepOneAttackingLandUnit(keepOneAttackingLandUnit);
    calculator.setAmphibious(amphibious);
    calculator.setRetreatAfterRound(retreatAfterRound);
    calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft);
    calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
    calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
    calculators.add(calculator);
    return executor.submit(
        () -> {
          try {
            calculator.setGameData(IoUtils.readFromMemory(bytes, GameDataManager::loadGame));
            return calculator.calculate(
                attacker,
                defender,
                location,
                attacking,
                defending,
                bombarding,
                territoryEffects,
                retreatWhenOnlyAirLeft,
                runs);
          } catch (final IOException e) {
            throw new RuntimeException("Failed to deserialize", e);
          } finally {
            calculators.remove(calculator);
          }
        });
  }

  private static AggregateResults aggregateResults(
      final List<Future<AggregateResults>> results, final int runsPerWorker) {
    final AggregateResults result = new AggregateResults(runsPerWorker);
    for (final Future<AggregateResults> future : results) {
      try {
        result.addResults(future.get().getResults());
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final ExecutionException e) {
        throw new IllegalStateException("Exception from worker", e);
      }
    }
    return result;
  }

  public void setKeepOneAttackingLandUnit(final boolean bool) {
    synchronized (mutex) {
      keepOneAttackingLandUnit = bool;
    }
  }

  public void setAmphibious(final boolean bool) {
    synchronized (mutex) {
      amphibious = bool;
    }
  }

  public void setRetreatAfterRound(final int value) {
    synchronized (mutex) {
      retreatAfterRound = value;
    }
  }

  public void setRetreatAfterXUnitsLeft(final int value) {
    synchronized (mutex) {
      retreatAfterXUnitsLeft = value;
    }
  }

  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    synchronized (mutex) {
      this.attackerOrderOfLosses = attackerOrderOfLosses;
    }
  }

  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    synchronized (mutex) {
      this.defenderOrderOfLosses = defenderOrderOfLosses;
    }
  }

  // not on purpose, we need to be able to cancel at any time
  public void cancel() {
    synchronized (mutex) {
      calculators.forEach(BattleCalculator::cancel);
    }
  }
}
