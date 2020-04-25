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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.java.Log;
import org.triplea.io.IoUtils;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up
 * the run count across these workers. This is mainly to be used by AIs since they call the
 * OddsCalculator a lot.
 */
@Log
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
  private boolean retreatWhenOnlyAirLeft = false;
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

  @Override
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

  @Override
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
      final int runCount)
      throws IllegalStateException {
    Preconditions.checkState(!isShutDown, "ConcurrentBattleCalculator is already shut down");
    Preconditions.checkState(bytes.length != 0, "Data has not been set yet.");
    final long start = System.currentTimeMillis();
    final List<Future<AggregateResults>> results = new ArrayList<>();
    final int runsPerWorker = runCount / MAX_THREADS;
    synchronized (mutex) {
      int remainingRuns = runCount;
      while (remainingRuns > 0) {
        final int individualRemaining = Math.min(remainingRuns, runsPerWorker);
        remainingRuns -= runsPerWorker;
        BattleCalculator calculator = new BattleCalculator();
        calculator.setKeepOneAttackingLandUnit(keepOneAttackingLandUnit);
        calculator.setAmphibious(amphibious);
        calculator.setRetreatAfterRound(retreatAfterRound);
        calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft);
        calculator.setRetreatWhenOnlyAirLeft(retreatWhenOnlyAirLeft);
        calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
        calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
        calculators.add(calculator);
        results.add(
            executor.submit(
                () -> {
                  try {
                    calculator.setGameData(
                        IoUtils.readFromMemory(bytes, GameDataManager::loadGame));
                    return calculator.calculate(
                        attacker,
                        defender,
                        location,
                        attacking,
                        defending,
                        bombarding,
                        territoryEffects,
                        individualRemaining);
                  } catch (final IOException e) {
                    throw new RuntimeException("Failed to deserialize", e);
                  } finally {
                    calculators.remove(calculator);
                  }
                }));
      }
    }
    final AggregateResults result = new AggregateResults(runsPerWorker);
    for (Future<AggregateResults> future : results) {
      try {
        result.addResults(future.get().getResults());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IllegalStateException("Exception from worker", e);
      }
    }
    result.setTime(System.currentTimeMillis() - start);
    return result;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    synchronized (mutex) {
      keepOneAttackingLandUnit = bool;
    }
  }

  @Override
  public void setAmphibious(final boolean bool) {
    synchronized (mutex) {
      amphibious = bool;
    }
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    synchronized (mutex) {
      retreatAfterRound = value;
    }
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    synchronized (mutex) {
      retreatAfterXUnitsLeft = value;
    }
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    synchronized (mutex) {
      retreatWhenOnlyAirLeft = value;
    }
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    synchronized (mutex) {
      this.attackerOrderOfLosses = attackerOrderOfLosses;
    }
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    synchronized (mutex) {
      this.defenderOrderOfLosses = defenderOrderOfLosses;
    }
  }

  // not on purpose, we need to be able to cancel at any time
  @Override
  public void cancel() {
    synchronized (mutex) {
      calculators.forEach(BattleCalculator::cancel);
    }
  }
}
