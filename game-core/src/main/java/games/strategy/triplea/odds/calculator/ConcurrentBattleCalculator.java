package games.strategy.triplea.odds.calculator;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.java.Log;
import org.triplea.io.IoUtils;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up
 * the run count across these workers. This is mainly to be used by AIs since they call the
 * OddsCalculator a lot.
 */
@Log
public class ConcurrentBattleCalculator implements IBattleCalculator {
  private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

  private final ExecutorService executor;
  // do not let calc start until it is set
  private volatile boolean isDataSet = false;
  // shortcut everything if we are shutting down
  private volatile boolean isShutDown = false;
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
                .setNameFormat(threadNamePrefix + " ConcurrentOddsCalculator Worker-%d")
                .build());
    this.dataLoadedAction = dataLoadedAction;
  }

  @Override
  public void setGameData(final GameData data) {
    bytes = data == null ? new byte[0] : GameDataUtils.serializeGameDataWithoutHistory(data);
    isDataSet = data != null;
    dataLoadedAction.run();
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
  public AggregateResults calculate(final GamePlayer attacker,
                                    final GamePlayer defender,
                                    final Territory location,
                                    final Collection<Unit> attacking,
                                    final Collection<Unit> defending,
                                    final Collection<Unit> bombarding,
                                    final Collection<TerritoryEffect> territoryEffects,
                                    final int runCount) throws IllegalStateException {
    final List<Future<AggregateResults>> results = new ArrayList<>();
    int remainingRuns = runCount;
    final int runsPerWorker = runCount / MAX_THREADS;
    while (remainingRuns > 0) {
      remainingRuns -= runsPerWorker;
      results.add(executor.submit(() -> {
        BattleCalculator calculator = new BattleCalculator();
        calculator.setKeepOneAttackingLandUnit(keepOneAttackingLandUnit);
        calculator.setAmphibious(amphibious);
        calculator.setRetreatAfterRound(retreatAfterRound);
        calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft);
        calculator.setRetreatWhenOnlyAirLeft(retreatWhenOnlyAirLeft);
        calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
        calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
        try {
          calculator.setGameData(IoUtils.readFromMemory(bytes, GameDataManager::loadGame));
        } catch (final IOException e) {
          throw new RuntimeException("Failed to deserialize", e);
        }
        return calculator.calculate(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runsPerWorker);
      }));
    }
    final AggregateResults result = new AggregateResults(runsPerWorker);
    for (Future<AggregateResults> future : results) {
      try {
        final AggregateResults currentResult = future.get();
        result.addResults(currentResult.getResults());
        result.setTime(result.getTime() + currentResult.getTime());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IllegalStateException("Exception from worker", e);
      }
    }
    return result;
  }

  @Override
  public boolean getIsReady() {
    return isDataSet && !isShutDown;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    keepOneAttackingLandUnit = bool;
  }

  @Override
  public void setAmphibious(final boolean bool) {
    amphibious = bool;
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    retreatAfterRound = value;
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    retreatAfterXUnitsLeft = value;
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    retreatWhenOnlyAirLeft = value;
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    this.attackerOrderOfLosses = attackerOrderOfLosses;
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    this.defenderOrderOfLosses = defenderOrderOfLosses;
  }

  // not on purpose, we need to be able to cancel at any time
  @Override
  public void cancel() {}
}
