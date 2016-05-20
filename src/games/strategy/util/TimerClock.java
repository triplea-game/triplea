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
 */
// this is an ill-fated at a shot clock for the game...
public class TimerClock<T> extends Observable {
  public interface ITimerClockNotification {
    int getSecondsLeft();

    boolean areWeInterrupting();
  }

  public TimerClock() {}

  public T start(final Runnable task, final T defaultReturnValue, final int interruptAfterSecondsIfNotFinished,
      final int delaySeconds, final Collection<Class<? extends RuntimeException>> exceptionsToIgnoreOnInterrupt,
      final Observer observer) {
    if (observer != null) {
      addObserver(observer);
    }
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean runnableFinishedSuccessfully = new AtomicBoolean(false);
    final AtomicBoolean runnableHadRuntimeException = new AtomicBoolean(false);
    // we want to catch exceptions and propagate them back up
    final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
    // start the task
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          task.run();
          runnableFinishedSuccessfully.set(true);
          if (latch != null) {
            latch.countDown();
          }
        } catch (final RuntimeException e) {
          exception.set(e);
          runnableHadRuntimeException.set(true);
          if (latch != null) {
            latch.countDown();
          }
        }
      }
    });
    t.start();

    final long delay = delaySeconds * 1000;
    // count every second
    final long period = 1000;
    final Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      int seconds = interruptAfterSecondsIfNotFinished;

      @Override
      public void run() {
        // update listeners
        TimerClock.this.setChanged();
        TimerClock.this.notifyObservers(new TimerClockNotification(seconds, false));
        // count down our timer
        if (seconds-- <= 0) {
          timer.cancel();
          if (latch != null) {
            latch.countDown();
          }
        }
      }
    }, delay, period);
    // wait for the latch
    if (latch != null) {
      try {
        latch.await();
      } catch (final InterruptedException e) {
        // if we are planning on interrupting this clock, we should change this
        e.printStackTrace();
      }
    }
    // interrupt the task if it is not yet done
    boolean interrupted = false;
    timer.cancel();
    if (!runnableFinishedSuccessfully.get() && !runnableHadRuntimeException.get()) {
      // notify listeners
      setChanged();
      notifyObservers(new TimerClockNotification(0, true));
      // wait a second to gracefully allow a remote player to receive the notice that they are out of time
      ThreadUtil.sleep(1000);
      interrupted = true;
      t.interrupt();
      try {
        t.join();
      } catch (final InterruptedException e) {
        // if we are planning on interrupting this clock, we should change this
        e.printStackTrace();
      }
    }
    deleteObservers();
    if (exception.get() != null
        && !(interrupted && exceptionsToIgnoreOnInterrupt.contains(exception.get().getClass()))) {
      // throw the exception back up
      throw exception.get();
    }
    // return default value if one is specified
    return defaultReturnValue;
  }

  public class TimerClockNotification implements ITimerClockNotification {
    public final int m_secondsLeft;
    public final boolean m_areWeInterrupting;

    public TimerClockNotification(final int secondsLeft, final boolean areWeInterrupting) {
      m_secondsLeft = secondsLeft;
      m_areWeInterrupting = areWeInterrupting;
    }

    @Override
    public int getSecondsLeft() {
      return m_secondsLeft;
    }

    @Override
    public boolean areWeInterrupting() {
      return m_areWeInterrupting;
    }

    @Override
    public String toString() {
      return "Seconds left: " + getSecondsLeft() + "   Interrupting: " + areWeInterrupting();
    }
  }
}
