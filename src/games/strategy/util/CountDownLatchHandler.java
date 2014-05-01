package games.strategy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A handler for CountDownLatch's with methods to release latches being waited on from outside of their threads.
 * Is Thread Safe.
 * 
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class CountDownLatchHandler
{
	private final List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
	private volatile boolean m_isShutDown = false;
	private final boolean m_releaseLatchOnInterrupt;
	
	public CountDownLatchHandler(final boolean releaseLatchOnInterrupt)
	{
		super();
		m_releaseLatchOnInterrupt = releaseLatchOnInterrupt;
	}
	
	/**
	 * If "releaseLatchOnInterrupt" was set to true (defaults to false) on construction of this handler, then interruptAll will release and remove all current latches.
	 * Otherwise does nothing.
	 */
	public void interruptAll()
	{
		if (m_releaseLatchOnInterrupt)
		{
			for (final CountDownLatch latch : m_latchesToCloseOnShutdown)
			{
				removeShutdownLatch(latch);
			}
		}
	}
	
	/**
	 * If "releaseLatchOnInterrupt" was set to true (defaults to false) on construction of this handler, then interruptLatch will release and remove the latch.
	 * Otherwise does nothing.
	 */
	public void interruptLatch(final CountDownLatch latch)
	{
		if (m_releaseLatchOnInterrupt)
		{
			removeShutdownLatch(latch);
		}
	}
	
	public boolean isShutDown()
	{
		return m_isShutDown;
	}
	
	/**
	 * Shuts down this handler by releasing all latches and clearing the list of latches being handled.
	 */
	public void shutDown()
	{
		synchronized (this)
		{
			if (m_isShutDown)
				return;
			m_isShutDown = true;
		}
		for (final CountDownLatch latch : m_latchesToCloseOnShutdown)
		{
			releaseLatch(latch);
		}
		m_latchesToCloseOnShutdown.clear();
	}
	
	/**
	 * Utility method to fully release any CountDownLatch.
	 */
	public static void releaseLatch(final CountDownLatch latch)
	{
		if (latch == null)
			return;
		while (latch.getCount() > 0)
		{
			latch.countDown();
		}
	}
	
	/**
	 * Add a latch that will be released when this handler shuts down.
	 * If this handler is already shutdown, then we will release the latch immediately.
	 */
	public void addShutdownLatch(final CountDownLatch latch)
	{
		synchronized (this)
		{
			if (m_isShutDown)
			{
				releaseLatch(latch);
				return;
			}
			m_latchesToCloseOnShutdown.add(latch);
		}
	}
	
	/**
	 * Releases the latch and removes it from the latches being handled by this handler.
	 */
	public void removeShutdownLatch(final CountDownLatch latch)
	{
		removeShutdownLatch(latch, false);
	}
	
	/**
	 * Removes the latch from the latches being handled by this handler, and will not release it if doNotRelease is true.
	 */
	public void removeShutdownLatch(final CountDownLatch latch, final boolean doNotRelease)
	{
		synchronized (this)
		{
			if (!doNotRelease)
			{
				releaseLatch(latch);
			}
			m_latchesToCloseOnShutdown.remove(latch);
		}
	}
}
