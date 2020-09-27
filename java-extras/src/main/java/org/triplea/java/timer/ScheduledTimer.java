package org.triplea.java.timer;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;

/**
 * Class for creating a class to be executed periodically. Sample usage:
 *
 * <pre><code>
 *   ScheduledTimer timer = Timers.newFixedRateTimer("thread-name")
 *      .period(20, TimeUnit.SECONDS)
 *      .delay(10, TimeUnit.SECONDS)
 *      .task(() -> executeTask());
 *   timer.start();
 *   timer.cancel();
 * </code></pre>
 *
 * The scheduled timer can also be started at time of creation:
 *
 * <pre><code>
 *   ScheduledTimer timer = Timers.newFixedRateTimer("thread-name")
 *      .period(20, TimeUnit.SECONDS)
 *      .task(() -> executeTask())
 *      .start();
 *   timer.cancel();
 * </code></pre>
 */
public class ScheduledTimer {
  private final Runnable task;
  private final long delayMillis;
  private final long periodMillis;

  private final Timer timer;
  @Getter private boolean running;

  ScheduledTimer(
      final String threadName,
      final Runnable task,
      final long delayMillis,
      final long periodMillis) {
    timer = Optional.ofNullable(threadName).map(Timer::new).orElseGet(Timer::new);
    this.task = task;
    this.delayMillis = delayMillis;
    this.periodMillis = periodMillis;
  }

  /**
   * Starts the timer and returns the started instance. The timer task will execute after the
   * configured delay period {@code delayMillis}.
   */
  public ScheduledTimer start() {
    running = true;
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            task.run();
          }
        },
        delayMillis,
        periodMillis);
    return this;
  }

  public void cancel() {
    running = false;
    timer.cancel();
  }
}
