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
	private final static Logger s_logger = Logger.getLogger(ConcurrentOddsCalculator.class.getName());
	
	private final int m_maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
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
	
	public ConcurrentOddsCalculator(final String threadNamePrefix)
	{
		m_executor = Executors.newFixedThreadPool(m_maxThreads, new DaemonThreadFactory(true, threadNamePrefix + " ConcurrentOddsCalculator Worker"));
		s_logger.fine("Initialized executor thread pool with size: " + m_maxThreads);
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
	
	private void createWorkers(final GameData data)
	{
		m_workers.clear();
		if (data != null && m_cancelCurrentOperation >= 0)
		{
			final long start = System.currentTimeMillis(); // see how long 1 copy takes (some games can get REALLY big)
			final GameData newData;
			try
			{ // make first copy, then release lock on it so game can continue (ie: we don't want to lock on it while we copy it 16 times, when once is enough)
				data.acquireReadLock(); // don't let the data change while we make the first copy
				newData = GameDataUtils.cloneGameData(data, false);
			} finally
			{
				data.releaseReadLock();
			}
			final long timeToCopyInMillis = (System.currentTimeMillis() - start);
			final int maxThreads;
			if (timeToCopyInMillis > 40000)
			{
				maxThreads = 1; // just use 1 thread if we took more than 30 seconds to copy
			}
			else if (timeToCopyInMillis > 5000)
			{
				maxThreads = Math.max(1, m_maxThreads / 2); // use half the number of threads available if we took more than 5 seconds to copy
			}
			else
			{
				maxThreads = m_maxThreads; // use all threads
			}
			try
			{
				newData.acquireReadLock(); // make sure all workers are using the same data
				int i = 0;
				while (m_cancelCurrentOperation >= 0 && i < maxThreads)
				{
					m_workers.add(new OddsCalculator(newData, (maxThreads == ++i))); // the last one will use our already copied data from above, without copying it again
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
		}
		m_latchWorkerThreadsCreation.countDown(); // allow setting new data to take place if it is waiting on us
		m_latchSetData.countDown(); // allow calcing and other stuff to go ahead
		s_logger.fine("Initialized worker thread pool with size: " + m_workers.size());
	}
	
	public void shutdown()
	{
		m_isShutDown = true;
		m_cancelCurrentOperation = Integer.MIN_VALUE / 2;
		// m_latchSetData.await(2000, TimeUnit.MILLISECONDS);
		cancel();
		m_executor.shutdown();
	}
	
	public void shutdownNow()
	{
		m_isShutDown = true;
		m_cancelCurrentOperation = Integer.MIN_VALUE / 2;
		cancel();
		m_executor.shutdownNow();
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
