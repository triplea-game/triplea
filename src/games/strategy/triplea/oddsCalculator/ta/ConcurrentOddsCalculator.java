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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.util.CountUpAndDownLatch;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up the run count
 * across these workers. This is mainly to be used by AIs since they call the OddsCalculator a lot.
 */
public class ConcurrentOddsCalculator implements IOddsCalculator {
  private static final Logger s_logger = Logger.getLogger(ConcurrentOddsCalculator.class.getName());
  private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());
  private int m_currentThreads = MAX_THREADS;
  private final ExecutorService m_executor;
  private final CopyOnWriteArrayList<OddsCalculator> m_workers = new CopyOnWriteArrayList<>();
  // do not let calc be set up til data is set
  private volatile boolean m_isDataSet = false;
  // do not let calc start until it is set
  private volatile boolean m_isCalcSet = false;
  // shortcut everything if we are shutting down
  private volatile boolean m_isShutDown = false;
  // shortcut setting of previous game data if we are trying to set it to a new one, or shutdown
  private volatile int m_cancelCurrentOperation = 0;
  // do not let calcing happen while we are setting game data
  private final CountUpAndDownLatch m_latchSetData = new CountUpAndDownLatch();
  // do not let setting of game data happen multiple times while we offload creating workers and copying data to a
  // different thread
  private final CountUpAndDownLatch m_latchWorkerThreadsCreation = new CountUpAndDownLatch();

  // do not let setting of game data happen at same time
  private final Object m_mutexSetGameData = new Object();
  // do not let multiple calculations or setting calc data happen at same time
  private final Object m_mutexCalcIsRunning = new Object();
  private final List<OddsCalculatorListener> m_listeners = new ArrayList<>();

  public ConcurrentOddsCalculator(final String threadNamePrefix) {
    m_executor = Executors.newFixedThreadPool(MAX_THREADS,
        new DaemonThreadFactory(true, threadNamePrefix + " ConcurrentOddsCalculator Worker"));
    s_logger.fine("Initialized executor thread pool with size: " + MAX_THREADS);
  }

  @Override
  public void setGameData(final GameData data) {
    // increment so that a new calc doesn't take place (since they all wait on this latch)
    m_latchSetData.increment();
    // cancel any current setting of data
    --m_cancelCurrentOperation;
    // cancel any existing calcing (it won't stop immediately, just quicker)
    cancel();
    synchronized (m_mutexSetGameData) {
      try {
        // since setting data takes place on a different thread, this is our token. wait on it since
        m_latchWorkerThreadsCreation.await();
        // we could have exited the synchronized block already.
      } catch (final InterruptedException e) {
      }
      cancel();
      m_isDataSet = false;
      m_isCalcSet = false;
      if (data == null || m_isShutDown) {
        m_workers.clear();
        ++m_cancelCurrentOperation;
        // allow calcing and other stuff to go ahead
        m_latchSetData.countDown();
      } else {
        ++m_cancelCurrentOperation;
        // increment our token, so that we can set the data in a different thread and return from this one
        m_latchWorkerThreadsCreation.increment();
        m_executor.submit(new Runnable() {
          @Override
          public void run() {
            createWorkers(data);
          }
        });
      }
    }
  }

  @Override
  public int getThreadCount() {
    return m_currentThreads;
  }

  // use both time and memory left to determine how many copies to make
  private static int getThreadsToUse(final long timeToCopyInMillis, final long memoryUsedBeforeCopy) {
    if (timeToCopyInMillis > 20000 || MAX_THREADS == 1) {
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
    m_workers.clear();
    if (data != null && m_cancelCurrentOperation >= 0) {
      // see how long 1 copy takes (some games can get REALLY big)
      final long startTime = System.currentTimeMillis();
      final long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      final GameData newData;
      try { // make first copy, then release lock on it so game can continue (ie: we don't want to lock on it while we
            // copy it 16 times,
            // when once is enough)
        // don't let the data change while we make the first copy
        data.acquireReadLock();
        newData = GameDataUtils.cloneGameData(data, false);
      } finally {
        data.releaseReadLock();
      }
      m_currentThreads = getThreadsToUse((System.currentTimeMillis() - startTime), startMemory);
      try {
        // make sure all workers are using the same data
        newData.acquireReadLock();
        int i = 0;
        if (m_currentThreads <= 2 || MAX_THREADS <= 2) // we are already in 1 executor thread, so we have MAX_THREADS-1
                                                       // threads left to use
        { // if 2 or fewer threads, do not multi-thread the copying (we have already copied it once above, so at most
          // only 1 more copy to
          // make)
          while (m_cancelCurrentOperation >= 0 && i < m_currentThreads) {
            // the last one will use our already copied data from above, without copying it again
            m_workers.add(new OddsCalculator(newData, (m_currentThreads == ++i)));
          }
        } else { // multi-thread our copying, cus why the heck not (it increases the speed of copying by about double)
          final CountDownLatch workerLatch = new CountDownLatch(m_currentThreads - 1);
          while (i < (m_currentThreads - 1)) {
            ++i;
            m_executor.submit(new Runnable() {
              @Override
              public void run() {
                if (m_cancelCurrentOperation >= 0) {
                  m_workers.add(new OddsCalculator(newData, false));
                }
                workerLatch.countDown();
              }
            });
          }
          // the last one will use our already copied data from above, without copying it again
          m_workers.add(new OddsCalculator(newData, true));
          try {
            workerLatch.await();
          } catch (final InterruptedException e) {
          }
        }
      } finally {
        newData.releaseReadLock();
      }
    }
    if (m_cancelCurrentOperation < 0 || data == null) {
      // we could have cancelled while setting data, so clear the workers again if so
      m_workers.clear();
      m_isDataSet = false;
    } else {
      // should make sure that all workers have their game data set before we can call calculate and other things
      m_isDataSet = true;
      notifyListenersGameDataIsSet();
    }
    // allow setting new data to take place if it is waiting on us
    m_latchWorkerThreadsCreation.countDown();
    // allow calcing and other stuff to go ahead
    m_latchSetData.countDown();
    s_logger.fine("Initialized worker thread pool with size: " + m_workers.size());
  }

  @Override
  public void shutdown() {
    m_isShutDown = true;
    m_cancelCurrentOperation = Integer.MIN_VALUE / 2;
    cancel();
    m_executor.shutdown();
    synchronized (m_listeners) {
      m_listeners.clear();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    shutdown();
    super.finalize();
  }

  private void awaitLatch() {
    try {
      // there is a small chance calculate or setCalculateData or something could be called in between calls to
      // setGameData
      m_latchSetData.await();
    } catch (final InterruptedException e) {
    }
  }

  @Override
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attacking, final Collection<Unit> defending, final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects, int runCount) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      m_isCalcSet = false;
      final int workerNum = m_workers.size();
      final int workerRunCount = Math.max(1, (runCount / Math.max(1, workerNum)));
      for (final OddsCalculator worker : m_workers) {
        if (!m_isDataSet || m_isShutDown) {
          // we could have attempted to set a new game data, while the old one was still being set, causing it to abort
          // with null data
          return;
        }
        worker.setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects,
            (runCount <= 0 ? 0 : workerRunCount));
        runCount -= workerRunCount;
      }
      if (!m_isDataSet || m_isShutDown || workerNum <= 0) {
        return;
      }
      m_isCalcSet = true;
    }
  }

  /**
   * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits
   * for all the future
   * results and combines them together.
   */
  @Override
  public AggregateResults calculate() throws IllegalStateException {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      final long start = System.currentTimeMillis();
      // Create worker thread pool and start all workers
      int totalRunCount = 0;
      final List<Future<AggregateResults>> list = new ArrayList<>();
      for (final OddsCalculator worker : m_workers) {
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
          final Future<AggregateResults> workerResult = m_executor.submit(worker);
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
        s_logger.log(Level.SEVERE, interruptExceptions.size() + " Battle results workers interrupted",
            interruptExceptions.iterator().next());
      }
      if (!executionExceptions.isEmpty()) {
        Exception e = null;
        for (final Set<ExecutionException> entry : executionExceptions.values()) {
          if (!entry.isEmpty()) {
            e = entry.iterator().next();
            s_logger.log(Level.SEVERE, entry.size() + " Battle results workers aborted by exception", e.getCause());
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
    synchronized (m_mutexCalcIsRunning) {
      setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
      return calculate();
    }
  }

  @Override
  public boolean getIsReady() {
    return m_isDataSet && m_isCalcSet && !m_isShutDown;
  }

  @Override
  public int getRunCount() {
    int totalRunCount = 0;
    for (final OddsCalculator worker : m_workers) {
      totalRunCount += worker.getRunCount();
    }
    return totalRunCount;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setKeepOneAttackingLandUnit(bool);
      }
    }
  }

  @Override
  public void setAmphibious(final boolean bool) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setAmphibious(bool);
      }
    }
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setRetreatAfterRound(value);
      }
    }
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setRetreatAfterXUnitsLeft(value);
      }
    }
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setRetreatWhenOnlyAirLeft(value);
      }
    }
  }

  @Override
  public void setRetreatWhenMetaPowerIsLower(final boolean value) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setRetreatWhenMetaPowerIsLower(value);
      }
    }
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
      }
    }
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    synchronized (m_mutexCalcIsRunning) {
      awaitLatch();
      for (final OddsCalculator worker : m_workers) {
        worker.setDefenderOrderOfLosses(defenderOrderOfLosses);
      }
    }
  }

  // not on purpose, we need to be able to cancel at any time
  @Override
  public void cancel() {
    for (final OddsCalculator worker : m_workers) {
      worker.cancel();
    }
  }

  @Override
  public void addOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  @Override
  public void removeOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (m_listeners) {
      m_listeners.remove(listener);
    }
  }

  private void notifyListenersGameDataIsSet() {
    synchronized (m_listeners) {
      for (final OddsCalculatorListener listener : m_listeners) {
        listener.dataReady();
      }
    }
  }
}


/**
 * Borrowed from Executors$DefaultThreadFactory, but allows for custom name and daemon.
 */
class DaemonThreadFactory implements ThreadFactory {
  private static final AtomicInteger poolNumber = new AtomicInteger(1);
  private final ThreadGroup group;
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;
  private final boolean daemon;

  DaemonThreadFactory(final boolean isDaemon, final String name) {
    daemon = isDaemon;
    final SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = name + ": pool-" + poolNumber.getAndIncrement() + "-thread-";
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    t.setDaemon(daemon);
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }
}
