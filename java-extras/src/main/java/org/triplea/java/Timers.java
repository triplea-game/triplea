package org.triplea.java;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** Factory class for creating timers to execute recurring tasks. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Timers {

  /**
   * Returns a type-safe builder to create a timer that executes a given task at a regular periodic
   * frequency.
   */
  public static Builders.FixedRateTimerPeriodBuilder fixedRateTimer() {
    return new Builders.FixedRateTimerPeriodBuilder();
  }

  // Builders inner class is to improve the external API so that the builder classes are not visible
  private static class Builders {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerPeriodBuilder {
      public FixedRateTimerOptionalDelayBuilder period(final long period, final TimeUnit timeUnit) {
        Preconditions.checkArgument(period > 0);
        Preconditions.checkArgument(timeUnit != null);
        return period(timeUnit.toMillis(period));
      }

      public FixedRateTimerOptionalDelayBuilder period(final long periodInMillis) {
        Preconditions.checkArgument(periodInMillis > 0);
        return new FixedRateTimerOptionalDelayBuilder(periodInMillis);
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerOptionalDelayBuilder {
      private final long periodMillis;

      public FixedRateTimerTaskBuilder delay(final long delay, final TimeUnit timeUnit) {
        Preconditions.checkArgument(delay >= 0);
        Preconditions.checkArgument(timeUnit != null);
        return delay(timeUnit.toMillis(delay));
      }

      public FixedRateTimerTaskBuilder delay(final long delayMillis) {
        Preconditions.checkArgument(delayMillis >= 0);
        return new FixedRateTimerTaskBuilder(periodMillis, delayMillis);
      }

      public Timer task(final Runnable task) {
        Preconditions.checkArgument(task != null);
        return new FixedRateTimerBuilder(periodMillis, null, task).build();
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerTaskBuilder {
      private final long periodMillis;
      @Nullable private final Long delayMillis;

      public Timer task(final Runnable task) {
        Preconditions.checkArgument(task != null);
        return new FixedRateTimerBuilder(periodMillis, delayMillis, task).build();
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FixedRateTimerBuilder {
      private final long periodMillis;
      @Nullable private final Long delayMillis;
      private final Runnable task;

      public Timer build() {
        final Timer timer = new Timer();

        timer.scheduleAtFixedRate(
            new TimerTask() {
              @Override
              public void run() {
                task.run();
              }
            },
            Optional.ofNullable(delayMillis).orElse(0L),
            periodMillis);
        return timer;
      }
    }
  }
}
