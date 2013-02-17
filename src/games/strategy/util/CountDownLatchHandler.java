package games.strategy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchHandler
{
	private final List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
	private boolean m_isShutDown;
	private boolean m_shutDownOnInterrupt = false;
	
	public CountDownLatchHandler(final boolean releaseLatchOnInterrupt)
	{
		super();
		m_shutDownOnInterrupt = releaseLatchOnInterrupt;
	}
	
	public void interruptAll()
	{
		if (m_shutDownOnInterrupt)
			shutDown();
	}
	
	public void interruptLatch(final CountDownLatch latch)
	{
		if (m_shutDownOnInterrupt)
			releaseLatch(latch);
	}
	
	public boolean isShutDown()
	{
		return m_isShutDown;
	}
	
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
	
	private void releaseLatch(final CountDownLatch latch)
	{
		if (latch == null)
			return;
		while (latch.getCount() > 0)
		{
			latch.countDown();
		}
	}
	
	/**
	 * Add a latch that will be released when the game shuts down.
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
	
	public void removeShutdownLatch(final CountDownLatch latch)
	{
		synchronized (this)
		{
			releaseLatch(latch);
			m_latchesToCloseOnShutdown.remove(latch);
		}
	}
}
