package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.util.CountUpAndDownLatch;

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

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up the run count
 * across these workers. This is mainly to be used by AIs since they call the OddsCalculator a lot.
 * 
 * @author Ron Murhammer (redrum) & Mark Christopher Duncan (veqryn)
 * @since 2014
 */
public class ConcurrentOddsCalculator implements IOddsCalculator
{
	private static final Logger s_logger = Logger.getLogger(ConcurrentOddsCalculator.class.getName());
	private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());
	
	private int m_currentThreads = MAX_THREADS;
	private final ExecutorService m_executor;
	private final CopyOnWriteArrayList<OddsCalculator> m_workers = new CopyOnWriteArrayList<OddsCalculator>();
	private volatile boolean m_isDataSet = false; // do not let calc be set up til data is set
	private volatile boolean m_isCalcSet = false; // do not let calc start until it is set
	private volatile boolean m_isShutDown = false; // shortcut everything if we are shutting down
	private volatile int m_cancelCurrentOperation = 0; // shortcut setting of previous game data if we are trying to set it to a new one, or shutdown
	private final CountUpAndDownLatch m_latchSetData = new CountUpAndDownLatch(); // do not let calcing happen while we are setting game data
	private final CountUpAndDownLatch m_latchWorkerThreadsCreation = new CountUpAndDownLatch(); // do not let setting of game data happen multiple times while we offload creating workers and copying data to a different thread
	private final Object m_mutexSetGameData = new Object(); // do not let setting of game data happen at same time
	private final Object m_mutexCalcIsRunning = new Object(); // do not let multiple calculations or setting calc data happen at same time
	private final List<OddsCalculatorListener> m_listeners = new ArrayList<OddsCalculatorListener>();
	
	public ConcurrentOddsCalculator(final String threadNamePrefix)
	{
		m_executor = Executors.newFixedThreadPool(MAX_THREADS, new DaemonThreadFactory(true, threadNamePrefix + " ConcurrentOddsCalculator Worker"));
		s_logger.fine("Initialized executor thread pool with size: " + MAX_THREADS);
	}
	
	public void setGameData(final GameData data)
	{
		m_latchSetData.increment(); // increment so that a new calc doesn't take place (since they all wait on this latch)
		--m_cancelCurrentOperation; // cancel any current setting of data
		cancel(); // cancel any existing calcing (it won't stop immediately, just quicker)
		synchronized (m_mutexSetGameData)
		{
			try
			{
				m_latchWorkerThreadsCreation.await(); // since setting data takes place on a different thread, this is our token. wait on it since we could have exited the synchronized block already.
			} catch (final InterruptedException e)
			{
			}
			cancel();
			m_isDataSet = false;
			m_isCalcSet = false;
			if (data == null || m_isShutDown)
			{
				m_workers.clear();
				++m_cancelCurrentOperation;
				m_latchSetData.countDown(); // allow calcing and other stuff to go ahead
			}
			else
			{
				++m_cancelCurrentOperation;
				m_latchWorkerThreadsCreation.increment();// increment our token, so that we can set the data in a different thread and return from this one
				m_executor.submit(new Runnable()
				{
					public void run()
					{
						createWorkers(data);
					}
				});
			}
		}
	}
	
	public int getThreadCount()
	{
		return m_currentThreads;
	}
	
	private static int getThreadsToUse(final long timeToCopyInMillis, final long memoryUsedBeforeCopy)
	{ // use both time and memory left to determine how many copies to make
		if (timeToCopyInMillis > 20000 || MAX_THREADS == 1)
			return 1; // just use 1 thread if we took more than 20 seconds to copy
		final Runtime runtime = Runtime.getRuntime();
		final long usedMemoryAfterCopy = runtime.totalMemory() - runtime.freeMemory();
		final long memoryLeftBeforeMax = runtime.maxMemory() - (Math.max(usedMemoryAfterCopy, memoryUsedBeforeCopy)); // we can not predict how the gc works
		final long memoryUsedByCopy = Math.max(100000, (usedMemoryAfterCopy - memoryUsedBeforeCopy)); // make sure it is a decent size regardless of how stupid the gc is
		final int numberOfTimesWeCanCopyMax = Math.max(1, (int) (Math.min(Integer.MAX_VALUE, (memoryLeftBeforeMax / memoryUsedByCopy)))); // we leave some memory left over just in case
		if (timeToCopyInMillis > 3000)
			return Math.min(numberOfTimesWeCanCopyMax, Math.max(1, (MAX_THREADS / 2))); // use half the number of threads available if we took more than 3 seconds to copy
		return Math.min(numberOfTimesWeCanCopyMax, MAX_THREADS); // use all threads
	}
	
	private void createWorkers(final GameData data)
	{
		m_workers.clear();
		if (data != null && m_cancelCurrentOperation >= 0)
		{
			final long startTime = System.currentTimeMillis(); // see how long 1 copy takes (some games can get REALLY big)
			final long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			final GameData newData;
			try
			{ // make first copy, then release lock on it so game can continue (ie: we don't want to lock on it while we copy it 16 times, when once is enough)
				data.acquireReadLock(); // don't let the data change while we make the first copy
				newData = GameDataUtils.cloneGameData(data, false);
			} finally
			{
				data.releaseReadLock();
			}
			m_currentThreads = getThreadsToUse((System.currentTimeMillis() - startTime), startMemory);
			try
			{
				newData.acquireReadLock(); // make sure all workers are using the same data
				int i = 0;
				if (m_currentThreads <= 2 || MAX_THREADS <= 2) // we are already in 1 executor thread, so we have MAX_THREADS-1 threads left to use
				{ // if 2 or fewer threads, do not multi-thread the copying (we have already copied it once above, so at most only 1 more copy to make)
					while (m_cancelCurrentOperation >= 0 && i < m_currentThreads)
					{
						m_workers.add(new OddsCalculator(newData, (m_currentThreads == ++i))); // the last one will use our already copied data from above, without copying it again
					}
				}
				else
				{ // multi-thread our copying, cus why the heck not (it increases the speed of copying by about double)
					final CountDownLatch workerLatch = new CountDownLatch(m_currentThreads - 1);
					while (i < (m_currentThreads - 1))
					{
						++i;
						m_executor.submit(new Runnable()
						{
							public void run()
							{
								if (m_cancelCurrentOperation >= 0)
								{
									m_workers.add(new OddsCalculator(newData, false));
								}
								workerLatch.countDown();
							}
						});
					}
					m_workers.add(new OddsCalculator(newData, true)); // the last one will use our already copied data from above, without copying it again
					try
					{
						workerLatch.await();
					} catch (final InterruptedException e)
					{
					}
				}
			} finally
			{
				newData.releaseReadLock();
			}
		}
		if (m_cancelCurrentOperation < 0 || data == null)
		{
			m_workers.clear(); // we could have cancelled while setting data, so clear the workers again if so
			m_isDataSet = false;
		}
		else
		{
			m_isDataSet = true;// should make sure that all workers have their game data set before we can call calculate and other things
			notifyListenersGameDataIsSet();
		}
		m_latchWorkerThreadsCreation.countDown(); // allow setting new data to take place if it is waiting on us
		m_latchSetData.countDown(); // allow calcing and other stuff to go ahead
		s_logger.fine("Initialized worker thread pool with size: " + m_workers.size());
	}
	
	public void shutdown()
	{
		m_isShutDown = true;
		m_cancelCurrentOperation = Integer.MIN_VALUE / 2;
		cancel();
		m_executor.shutdown();
		synchronized (m_listeners)
		{
			m_listeners.clear();
		}
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		shutdown();
		super.finalize();
	}
	
	private void awaitLatch()
	{
		try
		{
			m_latchSetData.await();// there is a small chance calculate or setCalculateData or something could be called in between calls to setGameData
		} catch (final InterruptedException e)
		{
		}
	}
	
	public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
				final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, int runCount)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			m_isCalcSet = false;
			final int workerNum = m_workers.size();
			final int workerRunCount = Math.max(1, (runCount / Math.max(1, workerNum)));
			for (final OddsCalculator worker : m_workers)
			{
				if (!m_isDataSet || m_isShutDown)
				{
					return;// we could have attempted to set a new game data, while the old one was still being set, causing it to abort with null data
				}
				worker.setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, (runCount <= 0 ? 0 : workerRunCount));
				runCount -= workerRunCount;
			}
			if (!m_isDataSet || m_isShutDown || workerNum <= 0)
			{
				return;
			}
			m_isCalcSet = true;
		}
	}
	
	/**
	 * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits for all the future results and combines them together.
	 */
	public AggregateResults calculate() throws IllegalStateException
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			final long start = System.currentTimeMillis();
			// Create worker thread pool and start all workers
			int totalRunCount = 0;
			final List<Future<AggregateResults>> list = new ArrayList<Future<AggregateResults>>();
			for (final OddsCalculator worker : m_workers)
			{
				if (!getIsReady())
				{
					return new AggregateResults(0);// we could have attempted to set a new game data, while the old one was still being set, causing it to abort with null data
				}
				if (!worker.getIsReady())
				{
					throw new IllegalStateException("Called calculate before setting calculate data!");
				}
				if (worker.getRunCount() > 0)
				{
					totalRunCount += worker.getRunCount();
					final Future<AggregateResults> workerResult = m_executor.submit(worker);
					list.add(workerResult);
				}
			}
			
			// Wait for all worker futures to complete and combine results
			final AggregateResults results = new AggregateResults(totalRunCount);
			final Set<InterruptedException> interruptExceptions = new HashSet<InterruptedException>();
			final Map<String, Set<ExecutionException>> executionExceptions = new HashMap<String, Set<ExecutionException>>();
			for (final Future<AggregateResults> future : list)
			{
				try
				{
					final AggregateResults result = future.get();
					results.addResults(result.getResults());
				} catch (final InterruptedException e)
				{
					interruptExceptions.add(e);
				} catch (final ExecutionException e)
				{
					final String cause = e.getCause().getLocalizedMessage();
					Set<ExecutionException> exceptions = executionExceptions.get(cause);
					if (exceptions == null)
						exceptions = new HashSet<ExecutionException>();
					exceptions.add(e);
					executionExceptions.put(cause, exceptions);
				}
			}
			// we don't want to scare the user with 8+ errors all for the same thing
			if (!interruptExceptions.isEmpty())
			{
				s_logger.log(Level.SEVERE, interruptExceptions.size() + " Battle results workers interrupted", interruptExceptions.iterator().next());
			}
			if (!executionExceptions.isEmpty())
			{
				Exception e = null;
				for (final Set<ExecutionException> entry : executionExceptions.values())
				{
					if (!entry.isEmpty())
					{
						e = entry.iterator().next();
						s_logger.log(Level.SEVERE, entry.size() + " Battle results workers aborted by exception", e.getCause());
					}
				}
				if (e != null)
					throw new IllegalStateException(e.getCause());
			}
			results.setTime(System.currentTimeMillis() - start);
			return results;
		}
	}
	
	public AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking,
				final Collection<Unit> defending, final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
			return calculate();
		}
	}
	
	public boolean getIsReady()
	{
		return m_isDataSet && m_isCalcSet && !m_isShutDown;
	}
	
	public int getRunCount()
	{
		int totalRunCount = 0;
		for (final OddsCalculator worker : m_workers)
		{
			totalRunCount += worker.getRunCount();
		}
		return totalRunCount;
	}
	
	public void setKeepOneAttackingLandUnit(final boolean bool)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setKeepOneAttackingLandUnit(bool);
			}
		}
	}
	
	public void setAmphibious(final boolean bool)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setAmphibious(bool);
			}
		}
	}
	
	public void setRetreatAfterRound(final int value)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setRetreatAfterRound(value);
			}
		}
	}
	
	public void setRetreatAfterXUnitsLeft(final int value)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setRetreatAfterXUnitsLeft(value);
			}
		}
	}
	
	public void setRetreatWhenOnlyAirLeft(final boolean value)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setRetreatWhenOnlyAirLeft(value);
			}
		}
	}
	
	public void setRetreatWhenMetaPowerIsLower(final boolean value)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setRetreatWhenMetaPowerIsLower(value);
			}
		}
	}
	
	public void setAttackerOrderOfLosses(final String attackerOrderOfLosses)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
			}
		}
	}
	
	public void setDefenderOrderOfLosses(final String defenderOrderOfLosses)
	{
		synchronized (m_mutexCalcIsRunning)
		{
			awaitLatch();
			for (final OddsCalculator worker : m_workers)
			{
				worker.setDefenderOrderOfLosses(defenderOrderOfLosses);
			}
		}
	}
	
	// not on purpose, we need to be able to cancel at any time
	public void cancel()
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.cancel();
		}
	}
	
	public void addOddsCalculatorListener(final OddsCalculatorListener listener)
	{
		synchronized (m_listeners)
		{
			m_listeners.add(listener);
		}
	}
	
	public void removeOddsCalculatorListener(final OddsCalculatorListener listener)
	{
		synchronized (m_listeners)
		{
			m_listeners.remove(listener);
		}
	}
	
	private void notifyListenersGameDataIsSet()
	{
		synchronized (m_listeners)
		{
			for (final OddsCalculatorListener listener : m_listeners)
			{
				listener.dataReady();
			}
		}
	}
}


/**
 * Borrowed from Executors$DefaultThreadFactory, but allows for custom name and daemon.
 * 
 * @author veqryn
 * 
 */
class DaemonThreadFactory implements ThreadFactory
{
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final boolean daemon;
	
	DaemonThreadFactory(final boolean isDaemon, final String name)
	{
		daemon = isDaemon;
		final SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = name + ": pool-" + poolNumber.getAndIncrement() + "-thread-";
	}
	
	public Thread newThread(final Runnable r)
	{
		final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		t.setDaemon(daemon);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}
