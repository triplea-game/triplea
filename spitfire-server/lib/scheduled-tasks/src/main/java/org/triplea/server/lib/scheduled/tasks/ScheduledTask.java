package org.triplea.server.lib.scheduled.tasks;

import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Use to run a background task on a recurring basis. The task is stopped when the server is
 * stopped. Typically used for things like periodic indexing jobs.
 */
@Slf4j
public class ScheduledTask implements Managed {

  private final String taskName;
  private final ScheduledTimer taskTimer;

  @Builder
  ScheduledTask(
      @Nonnull final String taskName,
      @Nonnull final Duration period,
      @Nonnull final Duration delay,
      @Nonnull final Runnable task) {
    this.taskName = taskName;
    taskTimer =
        Timers.fixedRateTimer(taskName)
            .period(period.toSeconds(), TimeUnit.SECONDS)
            .delay(delay.toSeconds(), TimeUnit.SECONDS)
            .task(task);
  }

  @Override
  public void start() {
    log.info("Starting scheduled task: {}", taskName);
    taskTimer.start();
  }

  @Override
  public void stop() {
    log.info("Stopping scheduled task: {}", taskName);
    taskTimer.cancel();
  }
}
