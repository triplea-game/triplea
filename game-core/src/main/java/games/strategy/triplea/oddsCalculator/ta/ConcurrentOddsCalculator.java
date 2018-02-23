package games.strategy.triplea.oddsCalculator.ta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.util.CountUpAndDownLatch;
import games.strategy.util.Interruptibles;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up the run count
 * across these workers. This is mainly to be used by AIs since they call the OddsCalculator a lot.
 */
public class ConcurrentOddsCalculator implements IOddsCalculator {
  private static final Logger logger = Logger.getLogger(ConcurrentOddsCalculator.class.getName());
  private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

  private int currentThreads = MAX_THREADS;
  private final ExecutorService executor;
  private final List<OddsCalculator> workers = new CopyOnWriteArrayList<>();
  // do not let calc be set up til data is set
  private volatile boolean isDataSet = false;
  // do not let calc start until it is set
  private volatile boolean isCalcSet = false;
  // shortcut everything if we are shutting down
  private volatile boolean isShutDown = false;
  // shortcut setting of previous game data if we are trying to set it to a new one, or shutdown
  private volatile int cancelCurrentOperation = 0;
  // do not let calcing happen while we are setting game data
  private final CountUpAndDownLatch latchSetData = new CountUpAndDownLatch();
  // do not let setting of game data happen multiple times while we offload creating workers and copying data to a
  // different thread
  private final CountUpAndDownLatch latchWorkerThreadsCreation = new CountUpAndDownLatch();

  // do not let setting of game data happen at same time
  private final Object mutexSetGameData = new Object();
  // do not let multiple calculations or setting calc data happen at same time
  private final Object mutexCalcIsRunning = new Object();
  private final List<OddsCalculatorListener> listeners = new ArrayList<>();

  public ConcurrentOddsCalculator(final String threadNamePrefix) {
    executor = Executors.newFixedThreadPool(MAX_THREADS,
        new DaemonThreadFactory(true, threadNamePrefix + " ConcurrentOddsCalculator Worker"));
  }

  @Override
  public void setGameData(final GameData data) {
    // increment so that a new calc doesn't take place (since they all wait on this latch)
    latchSetData.increment();
    // cancel any current setting of data
    --cancelCurrentOperation;
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
      isCalcSet = false;
      if ((data == null) || isShutDown) {
        workers.clear();
        ++cancelCurrentOperation;
        // allow calcing and other stuff to go ahead
        latchSetData.countDown();
      } else {
        ++cancelCurrentOperation;
        // increment our token, so that we can set the data in a different thread and return from this one
        latchWorkerThreadsCreation.increment();
        executor.submit(() -> createWorkers(data));
      }
    }
  }

  @Override
  public int getThreadCount() {
    return currentThreads;
  }

  // use both time and memory left to determine how many copies to make
  private static int getThreadsToUse(final long timeToCopyInMillis, final long memoryUsedBeforeCopy) {
    if ((timeToCopyInMillis > 20000) || (MAX_THREADS == 1)) {
      // just use 1 thread if we took more than 20 seconds to copy
      return 1;
    }
    final Runtime runtime = Runtime.getRuntime();
    final long usedMemoryAfterCopy = runtime.totalMemory() - runtime.freeMemory();
    // we cannot predict how the gc works
    final long memoryLeftBeforeMax = runtime.maxMemory() - (Math.max(usedMemoryAfterCopy, memoryUsedBeforeCopy));
    // make sure it is a decent size
    final long memoryUsedByCopy = Math.max(100000, (usedMemoryAfterCopy - memoryUsedBeforeCopy));
    // regardless of how stupid the gc is
    // we leave some memory left over just in case
    final int numberOfTimesWeCanCopyMax =
        Math.max(1, (int) (Math.min(Integer.MAX_VALUE, (memoryLeftBeforeMax / memoryUsedByCopy))));

    if (timeToCopyInMillis > 3000) {
      // use half the number of threads available if we took
      // more than 3 seconds to copy
      return Math.min(numberOfTimesWeCanCopyMax, Math.max(1, (MAX_THREADS / 2)));
    }
    // use all threads
    return Math.min(numberOfTimesWeCanCopyMax, MAX_THREADS);
  }

  private void createWorkers(final GameData data) {
    workers.clear();
    if ((data != null) && (cancelCurrentOperation >= 0)) {
      // see how long 1 copy takes (some games can get REALLY big)
      final long startTime = System.currentTimeMillis();
      final long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      final GameData newData;
      try {
        // make first copy, then release lock on it so game can continue (ie: we don't want to lock on it while we copy
        // it 16 times, when once is enough) don't let the data change while we make the first copy
        data.acquireReadLock();
        newData = GameDataUtils.cloneGameData(data, false);
      } finally {
        data.releaseReadLock();
      }
      currentThreads = getThreadsToUse((System.currentTimeMillis() - startTime), startMemory);
      try {
        // make sure all workers are using the same data
        newData.acquireReadLock();
        int i = 0;
        // we are already in 1 executor thread, so we have MAX_THREADS-1 threads left to use
        if ((currentThreads <= 2) || (MAX_THREADS <= 2)) {
          // if 2 or fewer threads, do not multi-thread the copying (we have already copied it once above, so at most
          // only 1 more copy to
          // make)
          while ((cancelCurrentOperation >= 0) && (i < currentThreads)) {
            // the last one will use our already copied data from above, without copying it again
            workers.add(new OddsCalculator(newData, (currentThreads == ++i)));
          }
        } else { // multi-thread our copying, cus why the heck not (it increases the speed of copying by about double)
          final CountDownLatch workerLatch = new CountDownLatch(currentThreads - 1);
          while (i < (currentThreads - 1)) {
            ++i;
            executor.submit(() -> {
              if (cancelCurrentOperation >= 0) {
                workers.add(new OddsCalculator(newData, false));
              }
              workerLatch.countDown();
            });
          }
          // the last one will use our already copied data from above, without copying it again
          workers.add(new OddsCalculator(newData, true));
          Interruptibles.await(workerLatch);
        }
      } finally {
        newData.releaseReadLock();
      }
    }
    if ((cancelCurrentOperation < 0) || (data == null)) {
      // we could have cancelled while setting data, so clear the workers again if so
      workers.clear();
      isDataSet = false;
    } else {
      // should make sure that all workers have their game data set before we can call calculate and other things
      isDataSet = true;
      notifyListenersGameDataIsSet();
    }
    // allow setting new data to take place if it is waiting on us
    latchWorkerThreadsCreation.countDown();
    // allow calcing and other stuff to go ahead
    latchSetData.countDown();
  }

  @Override
  public void shutdown() {
    isShutDown = true;
    cancelCurrentOperation = Integer.MIN_VALUE / 2;
    cancel();
    executor.shutdown();
    synchronized (listeners) {
      listeners.clear();
    }
  }

  private void awaitLatch() {
    try {
      // there is a small chance calculate or setCalculateData or something could be called in between calls to
      // setGameData
      latchSetData.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attacking, final Collection<Unit> defending, final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects, int runCount) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      isCalcSet = false;
      final int workerNum = workers.size();
      final int workerRunCount = Math.max(1, (runCount / Math.max(1, workerNum)));
      for (final OddsCalculator worker : workers) {
        if (!isDataSet || isShutDown) {
          // we could have attempted to set a new game data, while the old one was still being set, causing it to abort
          // with null data
          return;
        }
        worker.setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects,
            ((runCount <= 0) ? 0 : workerRunCount));
        runCount -= workerRunCount;
      }
      if (!isDataSet || isShutDown || (workerNum <= 0)) {
        return;
      }
      isCalcSet = true;
    }
  }

  /**
   * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits
   * for all the future
   * results and combines them together.
   */
  @Override
  public AggregateResults calculate() throws IllegalStateException {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      final long start = System.currentTimeMillis();
      // Create worker thread pool and start all workers
      int totalRunCount = 0;
      final List<Future<AggregateResults>> list = new ArrayList<>();
      for (final OddsCalculator worker : workers) {
        if (!getIsReady()) {
          // we could have attempted to set a new game data, while the old one was still being set, causing it to abort
          // with null data
          return new AggregateResults(0);
        }
        if (!worker.getIsReady()) {
          throw new IllegalStateException("Called calculate before setting calculate data!");
        }
        if (worker.getRunCount() > 0) {
          totalRunCount += worker.getRunCount();
          final Future<AggregateResults> workerResult = executor.submit(worker);
          list.add(workerResult);
        }
      }
      // Wait for all worker futures to complete and combine results
      final AggregateResults results = new AggregateResults(totalRunCount);
      final Set<InterruptedException> interruptExceptions = new HashSet<>();
      final Map<String, Set<ExecutionException>> executionExceptions = new HashMap<>();
      for (final Future<AggregateResults> future : list) {
        try {
          final AggregateResults result = future.get();
          results.addResults(result.getResults());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          interruptExceptions.add(e);
        } catch (final ExecutionException e) {
          final String cause = e.getCause().getLocalizedMessage();
          Set<ExecutionException> exceptions = executionExceptions.get(cause);
          if (exceptions == null) {
            exceptions = new HashSet<>();
          }
          exceptions.add(e);
          executionExceptions.put(cause, exceptions);
        }
      }
      // we don't want to scare the user with 8+ errors all for the same thing
      if (!interruptExceptions.isEmpty()) {
        logger.log(Level.SEVERE, interruptExceptions.size() + " Battle results workers interrupted",
            interruptExceptions.iterator().next());
      }
      if (!executionExceptions.isEmpty()) {
        Exception e = null;
        for (final Set<ExecutionException> entry : executionExceptions.values()) {
          if (!entry.isEmpty()) {
            e = entry.iterator().next();
            logger.log(Level.SEVERE, entry.size() + " Battle results workers aborted by exception", e.getCause());
          }
        }
        if (e != null) {
          throw new IllegalStateException(e.getCause());
        }
      }
      results.setTime(System.currentTimeMillis() - start);
      return results;
    }
  }

  @Override
  public AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender,
      final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
      final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount) {
    synchronized (mutexCalcIsRunning) {
      setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
      return calculate();
    }
  }

  @Override
  public boolean getIsReady() {
    return isDataSet && isCalcSet && !isShutDown;
  }

  @Override
  public int getRunCount() {
    int totalRunCount = 0;
    for (final OddsCalculator worker : workers) {
      totalRunCount += worker.getRunCount();
    }
    return totalRunCount;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setKeepOneAttackingLandUnit(bool);
      }
    }
  }

  @Override
  public void setAmphibious(final boolean bool) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setAmphibious(bool);
      }
    }
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setRetreatAfterRound(value);
      }
    }
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setRetreatAfterXUnitsLeft(value);
      }
    }
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setRetreatWhenOnlyAirLeft(value);
      }
    }
  }


  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
      }
    }
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    synchronized (mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : workers) {
        worker.setDefenderOrderOfLosses(defenderOrderOfLosses);
      }
    }
  }

  // not on purpose, we need to be able to cancel at any time
  @Override
  public void cancel() {
    for (final OddsCalculator worker : workers) {
      worker.cancel();
    }
  }

  @Override
  public void addOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  private void notifyListenersGameDataIsSet() {
    synchronized (listeners) {
      for (final OddsCalculatorListener listener : listeners) {
        listener.dataReady();
      }
    }
  }
}
