package org.triplea.maps.indexing;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Given a map indexing task, creates a schedule to run the indexing and once started will run at a
 * fixed rate until stopped.
 */
@Slf4j
public class MapsIndexingSchedule implements Managed {

  private final ScheduledTimer taskTimer;

  @Builder
  MapsIndexingSchedule(
      final int indexingPeriodMinutes, final MapIndexingTaskRunner mapIndexingTaskRunner) {
    taskTimer =
        Timers.fixedRateTimer("thread-name")
            .period(indexingPeriodMinutes, TimeUnit.MINUTES)
            .delay(10, TimeUnit.SECONDS)
            .task(mapIndexingTaskRunner);
  }

  @Override
  public void start() {
    log.info("Map indexing started");
    taskTimer.start();
  }

  @Override
  public void stop() {
    log.info("Map indexing stopped");
    taskTimer.cancel();
  }
}
