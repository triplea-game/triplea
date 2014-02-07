package games.strategy.util;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A way to put a timer on a runnable task. Instead of just interrupting the task,
 * we can also notify observers about the time left to complete, notify them that it completed successfully or not,
 * and we can also return a default object if needed.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
// this is an ill-fated at a shot clock for the game...
public class TimerClock<T> extends Observable
{
	public interface ITimerClockNotification
	{
		public int getSecondsLeft();
		
		public boolean areWeInterrupting();
	}
	
	public TimerClock()
	{
	}
	
	public static void startTask(final Runnable task, final int interruptAfterSecondsIfNotFinished, final int delaySeconds,
				final Collection<Class<? extends RuntimeException>> exceptionsToIgnoreOnInterrupt, final Observer observer)
	{
		new TimerClock<Object>().start(task, null, interruptAfterSecondsIfNotFinished, delaySeconds, exceptionsToIgnoreOnInterrupt, observer);
	}
	
	public T start(final Runnable task, final T defaultReturnValue, final int interruptAfterSecondsIfNotFinished, final int delaySeconds,
				final Collection<Class<? extends RuntimeException>> exceptionsToIgnoreOnInterrupt, final Observer observer)
	{
		if (observer != null)
			addObserver(observer);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean runnableFinishedSuccessfully = new AtomicBoolean(false);
		final AtomicBoolean runnableHadRuntimeException = new AtomicBoolean(false);
		final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>(); // we want to catch exceptions and propagate them back up
		// start the task
		final Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					// System.out.println("Starting: " + task.toString());
					task.run();
					// System.out.println("Finished: " + task.toString());
					runnableFinishedSuccessfully.set(true);
					if (latch != null)
						latch.countDown();
				} catch (final RuntimeException e)
				{
					// System.out.println("Task " + task.toString() + " threw exception: " + e.getMessage());
					// e.printStackTrace();
					exception.set(e);
					runnableHadRuntimeException.set(true);
					if (latch != null)
						latch.countDown();
				}
			}
		});
		t.start();
		
		// start the timer
		final long delay = delaySeconds * 1000;
		final long period = 1000; // count every second
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask()
		{
			int seconds = interruptAfterSecondsIfNotFinished;
			
			@Override
			public void run()
			{
				// update listeners
				TimerClock.this.setChanged();
				TimerClock.this.notifyObservers(new TimerClockNotification(seconds, false));
				// count down our timer
				// System.out.println("Seconds left: " + seconds);
				if (seconds-- <= 0)
				{
					timer.cancel();
					if (latch != null)
						latch.countDown();
				}
			}
		}, delay, period);
		
		// wait for the latch
		if (latch != null)
		{
			try
			{
				latch.await();
			} catch (final InterruptedException e)
			{
				// System.out.println("TimerClock latch interrupted");
				e.printStackTrace(); // if we are planning on interrupting this clock, we should change this
			}
		}
		
		// interrupt the task if it is not yet done
		boolean interrupted = false;
		timer.cancel();
		if (!runnableFinishedSuccessfully.get() && !runnableHadRuntimeException.get())
		{
			// System.out.println("Interrupting: " + task.toString());
			// notify listeners
			setChanged();
			notifyObservers(new TimerClockNotification(0, true));
			try
			{
				Thread.sleep(1000); // wait a second to gracefully allow a remote player to receive the notice that they are out of time
			} catch (final InterruptedException e1)
			{
				e1.printStackTrace();
			}
			interrupted = true;
			t.interrupt();
			try
			{
				t.join();
				// System.out.println("Joined: " + task.toString());
			} catch (final InterruptedException e)
			{
				e.printStackTrace(); // if we are planning on interrupting this clock, we should change this
			}
		}
		
		deleteObservers();
		if (exception.get() != null && !(interrupted && exceptionsToIgnoreOnInterrupt.contains(exception.get().getClass())))
		{
			// System.out.println("Throwing Exception: " + exception.get());
			throw exception.get(); // throw the exception back up
		}
		// return default value if one is specified
		return defaultReturnValue;
	}
	
	
	public class TimerClockNotification implements ITimerClockNotification
	{
		public final int m_secondsLeft;
		public final boolean m_areWeInterrupting;
		
		public TimerClockNotification(final int secondsLeft, final boolean areWeInterrupting)
		{
			m_secondsLeft = secondsLeft;
			m_areWeInterrupting = areWeInterrupting;
		}
		
		public int getSecondsLeft()
		{
			return m_secondsLeft;
		}
		
		public boolean areWeInterrupting()
		{
			return m_areWeInterrupting;
		}
		
		@Override
		public String toString()
		{
			return "Seconds left: " + getSecondsLeft() + "   Interrupting: " + areWeInterrupting();
		}
	}
}
