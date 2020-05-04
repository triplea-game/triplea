package games.strategy.triplea.odds.calculator;

import com.google.common.util.concurrent.Runnables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.triplea.java.Interruptibles;
import org.triplea.java.concurrency.CountUpAndDownLatch;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up
 * the run count across these workers. This is mainly to be used by AIs since they call the
 * OddsCalculator a lot.
 */
public class ConcurrentBattleCalculator implements IBattleCalculator {
  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();

  private final ExecutorService executor;
  private final List<BattleCalculator> workers = new CopyOnWriteArrayList<>();
  // do not let calc be set up til data is set
  private volatile boolean isDataSet = false;
  // shortcut everything if we are shutting down
  private volatile boolean isShutDown = false;
  // shortcut setting of previous game data if we are trying to set it to a new one, or shutdown
  private final AtomicInteger cancelCurrentOperation = new AtomicInteger(0);
  // do not let calcing happen while we are setting game data
  private final CountUpAndDownLatch latchSetData = new CountUpAndDownLatch();
  // do not let setting of game data happen multiple times while we offload creating workers and
  // copying data to a
  // different thread
  private final CountUpAndDownLatch latchWorkerThreadsCreation = new CountUpAndDownLatch();

  // do not let setting of game data happen at same time
  private final Object mutexSetGameData = new Object();
  // do not let multiple calculations or setting calc data happen at same time
  private final Object mutexCalcIsRunning = new Object();
  private final Runnable dataLoadedAction;

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
    // increment so that a new calc doesn't take place (since they all wait on this latch)
    latchSetData.increment();
    // cancel any current setting of data
    cancelCurrentOperation.decrementAndGet();
    // cancel any existing calcing (it won't stop immediately, just quicker)
    cancel();
    synchronized (mutexSetGameData) {
      try {
        // since setting data takes place on a different thread, this is our token. wait on it since
        latchWorkerThreadsCreation.await();
        // we could have exited the synchronized block already.
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      cancel();
      isDataSet = false;
      if (data == null || isShutDown) {
        workers.clear();
        cancelCurrentOperation.incrementAndGet();
        // allow calcing and other stuff to go ahead
        latchSetData.countDown();
      } else {
        cancelCurrentOperation.incrementAndGet();
        // increment our token, so that we can set the data in a different thread and return from
        // this one
        latchWorkerThreadsCreation.increment();
        executor.execute(() -> createWorkers(data));
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

  private void createWorkers(final GameData data) {
    workers.clear();
    if (data != null && cancelCurrentOperation.get() >= 0) {
      // see how long 1 copy takes (some games can get REALLY big)
      final long startTime = System.currentTimeMillis();
      final long startMemory =
          Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      final GameData newData;
      try {
        // make first copy, then release lock on it so game can continue (ie: we don't want to lock
        // on it while we copy
        // it 16 times, when once is enough) don't let the data change while we make the first copy
        data.acquireWriteLock();
        newData = GameDataUtils.cloneGameDataWithoutHistory(data, false);
      } finally {
        data.releaseWriteLock();
      }
      final int currentThreads =
          getThreadsToUse((System.currentTimeMillis() - startTime), startMemory);
      try {
        // make sure all workers are using the same data
        newData.acquireReadLock();
        int i = 0;
        // we are already in 1 executor thread, so we have MAX_THREADS-1 threads left to use
        if (currentThreads <= 2 || MAX_THREADS <= 2) {
          // if 2 or fewer threads, do not multi-thread the copying (we have already copied it once
          // above, so at most
          // only 1 more copy to make)
          while (cancelCurrentOperation.get() >= 0 && i < currentThreads) {
            // the last one will use our already copied data from above, without copying it again
            workers.add(new BattleCalculator(newData, (currentThreads == ++i)));
          }
        } else { // multi-thread our copying, cus why the heck not (it increases the speed of
          // copying by about double)
          final CountDownLatch workerLatch = new CountDownLatch(currentThreads - 1);
          while (i < (currentThreads - 1)) {
            ++i;
            executor.execute(
                () -> {
                  if (cancelCurrentOperation.get() >= 0) {
                    workers.add(new BattleCalculator(newData, false));
                  }
                  workerLatch.countDown();
                });
          }
          // the last one will use our already copied data from above, without copying it again
          workers.add(new BattleCalculator(newData, true));
          Interruptibles.await(workerLatch);
        }
      } finally {
        newData.releaseReadLock();
      }
    }
    if (cancelCurrentOperation.get() < 0 || data == null) {
      // we could have cancelled while setting data, so clear the workers again if so
      workers.clear();
      isDataSet = false;
    } else {
      // should make sure that all workers have their game data set before we can call calculate and
      // other things
      isDataSet = true;
      dataLoadedAction.run();
    }
    // allow setting new data to take place if it is waiting on us
    latchWorkerThreadsCreation.countDown();
    // allow calcing and other stuff to go ahead
    latchSetData.countDown();
  }

  public void shutdown() {
    isShutDown = true;
    cancelCurrentOperation.set(Integer.MIN_VALUE / 2);
    cancel();
    executor.shutdown();
  }

  private void awaitLatch() {
    try {
      // there is a small chance calculate or setCalculateData or something could be called in
      // between calls to setGameData
      latchSetData.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      final long start = System.currentTimeMillis();
      final var runCountDistributor = new RunCountDistributor(runCount, workers.size());
      final List<Future<AggregateResults>> list =
          workers.stream()
              .map(
                  worker ->
                      executor.submit(
                          () ->
                              worker.calculate(
                                  attacker,
                                  defender,
                                  location,
                                  attacking,
                                  defending,
                                  bombarding,
                                  territoryEffects,
                                  retreatWhenOnlyAirLeft,
                                  runCountDistributor.nextRunCount())))
              .collect(Collectors.toList());
      if (!getIsReady()) {
        // we could have attempted to set a new game data, while the old one was still being set,
        // causing it to abort with null data
        return new AggregateResults(0);
      }
      // Wait for all worker futures to complete and combine results
      final AggregateResults results = new AggregateResults(runCount);
      for (final Future<AggregateResults> future : list) {
        try {
          final AggregateResults result = future.get();
          results.addResults(result.getResults());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
          throw new IllegalStateException("Battle results workers aborted by exception", e);
        }
      }
      results.setTime(System.currentTimeMillis() - start);
      return results;
    }
  }

  public boolean getIsReady() {
    return isDataSet && !isShutDown;
  }

  public void setKeepOneAttackingLandUnit(final boolean bool) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final BattleCalculator worker : workers) {
        worker.setKeepOneAttackingLandUnit(bool);
      }
    }
  }

  public void setAmphibious(final boolean bool) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final BattleCalculator worker : workers) {
        worker.setAmphibious(bool);
      }
    }
  }

  public void setRetreatAfterRound(final int value) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final BattleCalculator worker : workers) {
        worker.setRetreatAfterRound(value);
      }
    }
  }

  public void setRetreatAfterXUnitsLeft(final int value) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final BattleCalculator worker : workers) {
        worker.setRetreatAfterXUnitsLeft(value);
      }
    }
  }

  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final BattleCalculator worker : workers) {
        worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
      }
    }
  }

  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
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
