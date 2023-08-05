package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up
 * the run count across these workers. This is mainly to be used by AIs since they call the
 * OddsCalculator a lot.
 */
@Slf4j
public class ConcurrentBattleCalculator implements IBattleCalculator {
  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  private final List<BattleCalculator> workers = new CopyOnWriteArrayList<>();
  // do not let calc be set up til data is set
  private volatile boolean isDataSet = false;
  // shortcut setting of previous game data if we are trying to set it to a new one, or shutdown
  private final AtomicInteger cancelCurrentOperation = new AtomicInteger(0);
  // do not let setting of game data happen multiple times while we offload creating workers and
  // copying data to a different thread
  private CompletableFuture<Boolean> latchWorkerThreadsCreation =
      CompletableFuture.completedFuture(false);

  // do not let setting of game data happen at same time
  private final Object mutexSetGameData = new Object();
  // do not let multiple calculations or setting calc data happen at same time
  private final Object mutexCalcIsRunning = new Object();

  /** Return value may be ignored. Exceptions are being handled properly. */
  public CompletableFuture<Boolean> setGameData(@Nullable final GameData data) {
    // cancel any current setting of data
    cancelCurrentOperation.decrementAndGet();
    // cancel any existing calcing (it won't stop immediately, just quicker)
    cancel();
    synchronized (mutexSetGameData) {
      waitForGameDataReady();
      latchWorkerThreadsCreation =
          CompletableFuture.supplyAsync(() -> setGameDataInternal(data))
              .exceptionally(
                  throwable -> {
                    log.error("Error while trying to set GameData", throwable);
                    return false;
                  });
      return latchWorkerThreadsCreation;
    }
  }

  private boolean setGameDataInternal(@Nullable final GameData data) {
    synchronized (mutexCalcIsRunning) {
      cancel();
      cancelCurrentOperation.incrementAndGet();
      isDataSet = createWorkers(data);
      return isDataSet;
    }
  }

  private void waitForGameDataReady() {
    synchronized (mutexSetGameData) {
      try {
        // since setting data takes place on a different thread, this is our token. wait on it since
        // we could have exited the synchronized block already.
        latchWorkerThreadsCreation.get();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new IllegalStateException("CompletableFuture should handle all exceptions", e);
      }
    }
  }

  // use both time and memory left to determine how many copies to make
  private static int getThreadsToUse(
      final long timeToCopyInMillis, final long memoryUsedBeforeCopy) {
    if (timeToCopyInMillis > 20000 || MAX_THREADS == 1) {
      // just use 1 thread if we took more than 20 seconds to copy
      return 1;
    }
    final Runtime runtime = Runtime.getRuntime();
    final long usedMemoryAfterCopy = runtime.totalMemory() - runtime.freeMemory();
    // we cannot predict how the gc works
    final long memoryLeftBeforeMax =
        runtime.maxMemory() - Math.max(usedMemoryAfterCopy, memoryUsedBeforeCopy);
    // make sure it is a decent size
    final long memoryUsedByCopy = Math.max(100000, (usedMemoryAfterCopy - memoryUsedBeforeCopy));
    // regardless of how stupid the gc is we leave some memory left over just in case
    final int numberOfTimesWeCanCopyMax =
        Math.max(1, (int) Math.min(Integer.MAX_VALUE, (memoryLeftBeforeMax / memoryUsedByCopy)));

    if (timeToCopyInMillis > 3000) {
      // use half the number of threads available if we took more than 3 seconds to copy
      return Math.min(numberOfTimesWeCanCopyMax, Math.max(1, (MAX_THREADS / 2)));
    }
    // use all threads
    return Math.min(numberOfTimesWeCanCopyMax, MAX_THREADS);
  }

  private boolean createWorkers(@Nullable final GameData data) {
    workers.clear();
    if (data != null && cancelCurrentOperation.get() >= 0) {
      // see how long 1 copy takes (some games can get REALLY big)
      final long startTime = System.currentTimeMillis();
      final long startMemory =
          Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      final byte[] serializedData;
      try (GameData.Unlocker ignored = data.acquireWriteLock()) {
        // Serialize the data, then release lock on it so game can continue (ie: we don't want to
        // lock on it while we copy it 16 times).
        serializedData =
            GameDataUtils.gameDataToBytes(data, GameDataManager.Options.forBattleCalculator())
                .orElse(null);
        if (serializedData == null) {
          return false;
        }
      }
      if (cancelCurrentOperation.get() >= 0) {
        // Create the first battle calc on the current thread to measure the end-to-end copy time.
        workers.add(new BattleCalculator(serializedData));
        int threadsToUse = getThreadsToUse((System.currentTimeMillis() - startTime), startMemory);
        // Now, create the remaining ones in parallel.
        workers.addAll(
            IntStream.range(1, threadsToUse)
                .parallel()
                .filter(j -> cancelCurrentOperation.get() >= 0)
                .mapToObj(j -> new BattleCalculator(serializedData))
                .collect(Collectors.toList()));
      }
    }
    if (cancelCurrentOperation.get() < 0 || data == null) {
      // we could have cancelled while setting data, so clear the workers again if so
      workers.clear();
      return false;
    }
    // should make sure that all workers have their game data set before
    // we can call calculate and other things
    return true;
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
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      final long start = System.currentTimeMillis();
      if (!isDataSet) {
        // we could have attempted to set a new game data, while the old one was still being set,
        // causing it to abort with null data
        return new AggregateResults(0);
      }
      final var runCountDistributor = new RunCountDistributor(runCount, workers.size());
      final AggregateResults results =
          new AggregateResults(
              workers.parallelStream()
                  .map(
                      worker ->
                          // Note: Although we're running in parallel, the data passed in does not
                          // get modified, so no copies are necessary. Also, the outer calculate()
                          // call is synchronous, so there's no problem if the caller later modifies
                          // the collections that were provided.
                          worker.calculate(
                              attacker,
                              defender,
                              location,
                              attacking,
                              defending,
                              bombarding,
                              territoryEffects,
                              retreatWhenOnlyAirLeft,
                              runCountDistributor.nextRunCount()))
                  .map(AggregateResults::getResults)
                  .flatMap(Collection::parallelStream)
                  .collect(Collectors.toList()));
      results.setTime(System.currentTimeMillis() - start);
      return results;
    }
  }

  public void setKeepOneAttackingLandUnit(final boolean bool) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setKeepOneAttackingLandUnit(bool);
      }
    }
  }

  public void setAmphibious(final boolean bool) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setAmphibious(bool);
      }
    }
  }

  public void setRetreatAfterRound(final int value) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setRetreatAfterRound(value);
      }
    }
  }

  public void setRetreatAfterXUnitsLeft(final int value) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setRetreatAfterXUnitsLeft(value);
      }
    }
  }

  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
      }
    }
  }

  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    waitForGameDataReady();
    synchronized (mutexCalcIsRunning) {
      for (final BattleCalculator worker : workers) {
        worker.setDefenderOrderOfLosses(defenderOrderOfLosses);
      }
    }
  }

  // not on purpose, we need to be able to cancel at any time
  public void cancel() {
    for (final BattleCalculator worker : workers) {
      worker.cancel();
    }
  }
}
