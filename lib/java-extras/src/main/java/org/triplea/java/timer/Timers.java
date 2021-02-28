package org.triplea.java.timer;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.triplea.java.ArgChecker;

/** Factory class for creating timers to execute recurring tasks. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Timers {
  /**
   * Returns a type-safe builder to create a timer that executes a given task at a regular periodic
   * frequency.
   *
   * @param threadName The name of the timer thread that will be created.
   */
  public static Builders.FixedRateTimerPeriodBuilder fixedRateTimer(final String threadName) {
    ArgChecker.checkNotEmpty(threadName);
    return new Builders.FixedRateTimerPeriodBuilder(threadName);
  }

  // Builders inner class is to improve the external API so that the builder classes are not visible
  private static class Builders {
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerPeriodBuilder {
      private final String threadName;

      public FixedRateTimerOptionalDelayBuilder period(final long period, final TimeUnit timeUnit) {
        Preconditions.checkArgument(period > 0);
        Preconditions.checkArgument(timeUnit != null);
        return period(timeUnit.toMillis(period));
      }

      public FixedRateTimerOptionalDelayBuilder period(final long periodInMillis) {
        Preconditions.checkArgument(periodInMillis > 0);
        return new FixedRateTimerOptionalDelayBuilder(threadName, periodInMillis);
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerOptionalDelayBuilder {
      private final String threadName;
      private final long periodMillis;

      public FixedRateTimerTaskBuilder delay(final long delay, final TimeUnit timeUnit) {
        Preconditions.checkArgument(delay >= 0);
        Preconditions.checkArgument(timeUnit != null);
        return delay(timeUnit.toMillis(delay));
      }

      public FixedRateTimerTaskBuilder delay(final long delayMillis) {
        Preconditions.checkArgument(delayMillis >= 0);
        return new FixedRateTimerTaskBuilder(threadName, periodMillis, delayMillis);
      }

      public ScheduledTimer task(final Runnable task) {
        Preconditions.checkArgument(task != null);
        return new FixedRateTimerBuilder(threadName, periodMillis, null, task).build();
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerTaskBuilder {
      private final String threadName;
      private final long periodMillis;
      @Nullable private final Long delayMillis;

      public ScheduledTimer task(final Runnable task) {
        Preconditions.checkArgument(task != null);
        return new FixedRateTimerBuilder(threadName, periodMillis, delayMillis, task).build();
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FixedRateTimerBuilder {
      private final String threadName;
      private final long periodMillis;
      @Nullable private final Long delayMillis;
      private final Runnable task;

      public ScheduledTimer build() {
        return new ScheduledTimer(
            threadName, task, Optional.ofNullable(delayMillis).orElse(0L), periodMillis);
      }
    }
  }
}
