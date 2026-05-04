package org.triplea.java.timer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** Factory class for creating timers to execute recurring tasks or one-off delayed tasks */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Timers {

  /**
   * Waits a fixed delay and then executes a given runnable.
   *
   * @param delay The number of units to delay (eg: 1, 2, 3)
   * @param delayTimeUnit The unit of the delay (eg: minutes, seconds).
   * @param runnable The task to be run.
   */
  public static void executeAfterDelay(
      final int delay, final TimeUnit delayTimeUnit, final Runnable runnable) {
    if (delay < 0) {
      throw new IllegalArgumentException("Delay must be non-negative, was " + delay);
    }
    new ScheduledThreadPoolExecutor(1).schedule(runnable, delay, delayTimeUnit);
  }

  /**
   * Returns a type-safe builder to create a timer that executes a given task at a regular periodic
   * frequency.
   *
   * @param threadName The name of the timer thread that will be created.
   */
  public static Builders.FixedRateTimerPeriodBuilder fixedRateTimer(final String threadName) {
    Objects.requireNonNull(threadName, "threadName cannot be null");
    if (threadName.isBlank()) {
      throw new IllegalArgumentException("threadName cannot be blank");
    }
    return new Builders.FixedRateTimerPeriodBuilder(threadName);
  }

  // Builders inner class is to improve the external API so that the builder classes are not visible
  private static class Builders {
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerPeriodBuilder {
      private final String threadName;

      public FixedRateTimerOptionalDelayBuilder period(final long period, final TimeUnit timeUnit) {
        if (period <= 0) {
          throw new IllegalArgumentException();
        }
        if (timeUnit == null) {
          throw new IllegalArgumentException();
        }
        return period(timeUnit.toMillis(period));
      }

      public FixedRateTimerOptionalDelayBuilder period(final long periodInMillis) {
        if (periodInMillis <= 0) {
          throw new IllegalArgumentException();
        }
        return new FixedRateTimerOptionalDelayBuilder(threadName, periodInMillis);
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerOptionalDelayBuilder {
      private final String threadName;
      private final long periodMillis;

      public FixedRateTimerTaskBuilder delay(final long delay, final TimeUnit timeUnit) {
        if (delay < 0) {
          throw new IllegalArgumentException();
        }
        if (timeUnit == null) {
          throw new IllegalArgumentException();
        }
        return delay(timeUnit.toMillis(delay));
      }

      public FixedRateTimerTaskBuilder delay(final long delayMillis) {
        if (delayMillis < 0) {
          throw new IllegalArgumentException();
        }
        return new FixedRateTimerTaskBuilder(threadName, periodMillis, delayMillis);
      }

      public ScheduledTimer task(final Runnable task) {
        if (task == null) {
          throw new IllegalArgumentException();
        }
        return new FixedRateTimerBuilder(threadName, periodMillis, null, task).build();
      }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FixedRateTimerTaskBuilder {
      private final String threadName;
      private final long periodMillis;
      @Nullable private final Long delayMillis;

      public ScheduledTimer task(final Runnable task) {
        if (task == null) {
          throw new IllegalArgumentException();
        }
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
