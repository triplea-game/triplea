package games.strategy.engine.framework.ui.background;

import games.strategy.util.CountDownLatchHandler;

import java.awt.Component;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class BackgroundTaskRunner
{
	public static void runInBackground(final Component parent, final String waitMessage, final Runnable r)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		final WaitDialog window = new WaitDialog(parent, waitMessage);
		final AtomicBoolean doneWait = new AtomicBoolean(false);
		final Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					r.run();
				} finally
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							doneWait.set(true);
							window.setVisible(false);
							window.dispose();
						}
					});
				}
			}
		});
		t.start();
		if (!doneWait.get())
		{
			window.pack();
			window.setLocationRelativeTo(parent);
			window.setVisible(true);
		}
	}
	
	public static void runInBackground(final Component parent, final String waitMessage, final Runnable r, final CountDownLatchHandler latchHandler)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			runInBackground(parent, waitMessage, r);
			return;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				runInBackground(parent, waitMessage, r);
				latch.countDown();
			}
		});
		if (latchHandler != null)
			latchHandler.addShutdownLatch(latch);
		boolean done = false;
		while (!done)
		{
			try
			{
				latch.await();
				done = true;
			} catch (final InterruptedException e)
			{
				if (latchHandler != null)
					latchHandler.interruptLatch(latch);
			}
		}
		if (latchHandler != null)
			latchHandler.removeShutdownLatch(latch);
		return;
	}
}
