package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	private final List<OddsCalculator> m_workers = new ArrayList<OddsCalculator>();
	private volatile boolean m_isDataSet = false;
	private volatile boolean m_isCalcSet = false;
	
	public ConcurrentOddsCalculator()
	{
		m_executor = Executors.newFixedThreadPool(m_maxThreads, new DaemonThreadFactory(true, "ConcurrentOddsCalculator Worker"));
		s_logger.fine("Initialized executor thread pool with size: " + m_maxThreads);
	}
	
	public synchronized void setGameData(final GameData data)
	{
		m_isDataSet = false;
		// i suppose there is a small chance calculate or setCalculateData or something could be called in between these synchronized calls to setGameData->createWorkers
		m_executor.submit(new Runnable()
		{
			public void run()
			{
				createWorkers(data);
				// synchronized should make sure that all workers have their game data set before we can call calculate and other things
			}
		});
	}
	
	private synchronized void createWorkers(final GameData data)
	{
		m_workers.clear();
		if (data != null)
		{
			try
			{
				data.acquireReadLock(); // make sure all workers are using the same data
				for (int i = 0; i < m_maxThreads; i++)
				{
					m_workers.add(new OddsCalculator(data));
				}
			} finally
			{
				data.releaseReadLock();
			}
		}
		m_isDataSet = data != null;
		s_logger.fine("Initialized worker thread pool with size: " + m_workers.size());
	}
	
	public synchronized void shutdown()
	{
		m_executor.shutdown();
	}
	
	public void shutdownNow()
	{
		m_executor.shutdownNow();
	}
	
	public synchronized void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
				final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount)
	{
		m_isCalcSet = false;
		final int workerRunCount = Math.max(Math.min(1, runCount), (runCount / m_workers.size()));
		for (final OddsCalculator worker : m_workers)
		{
			worker.setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, workerRunCount);
		}
		m_isCalcSet = true;
	}
	
	/**
	 * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits for all the future results and combines them together.
	 */
	public synchronized AggregateResults calculate()
	{
		if (!getIsReady())
		{
			throw new IllegalStateException("Called calculate before setting data and calculate data!");
		}
		final long start = System.currentTimeMillis();
		
		// Create worker thread pool and start all workers
		int totalRunCount = 0;
		final List<Future<AggregateResults>> list = new ArrayList<Future<AggregateResults>>();
		for (final OddsCalculator worker : m_workers)
		{
			if (!worker.getIsReady())
			{
				throw new IllegalStateException("Called calculate before setting calculate data!");
			}
			totalRunCount += worker.getRunCount();
			final Future<AggregateResults> workerResult = m_executor.submit(worker);
			list.add(workerResult);
		}
		
		// Wait for all worker futures to complete and combine results
		final AggregateResults results = new AggregateResults(totalRunCount);
		for (final Future<AggregateResults> future : list)
		{
			try
			{
				final AggregateResults result = future.get();
				results.addResults(result.getResults());
			} catch (final InterruptedException e)
			{
				s_logger.log(Level.SEVERE, "Battle results worker interrupted", e);
			} catch (final ExecutionException e)
			{
				s_logger.log(Level.SEVERE, "Battle results worker aborted by exception", e);
			}
		}
		
		results.setTime(System.currentTimeMillis() - start);
		
		return results;
	}
	
	public synchronized AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking,
				final Collection<Unit> defending, final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount)
	{
		setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
		return calculate();
	}
	
	public boolean getIsReady()
	{
		return m_isDataSet && m_isCalcSet;
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
	
	public synchronized void setKeepOneAttackingLandUnit(final boolean bool)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setKeepOneAttackingLandUnit(bool);
		}
	}
	
	public synchronized void setAmphibious(final boolean bool)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setAmphibious(bool);
		}
	}
	
	public synchronized void setRetreatAfterRound(final int value)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setRetreatAfterRound(value);
		}
	}
	
	public synchronized void setRetreatAfterXUnitsLeft(final int value)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setRetreatAfterXUnitsLeft(value);
		}
	}
	
	public synchronized void setRetreatWhenOnlyAirLeft(final boolean value)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setRetreatWhenOnlyAirLeft(value);
		}
	}
	
	public synchronized void setRetreatWhenMetaPowerIsLower(final boolean value)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setRetreatWhenMetaPowerIsLower(value);
		}
	}
	
	public synchronized void setAttackerOrderOfLosses(final String attackerOrderOfLosses)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setAttackerOrderOfLosses(attackerOrderOfLosses);
		}
	}
	
	public synchronized void setDefenderOrderOfLosses(final String defenderOrderOfLosses)
	{
		for (final OddsCalculator worker : m_workers)
		{
			worker.setDefenderOrderOfLosses(defenderOrderOfLosses);
		}
	}
	
	// not synchronized on purpose, we need to be able to cancel at any time
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
