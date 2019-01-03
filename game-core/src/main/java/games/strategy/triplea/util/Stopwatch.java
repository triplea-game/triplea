package games.strategy.triplea.util;

import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * Utility class for timing. Use, Stopwatch someTask = new StopWatch(someLogger, someLevel,
 * taskDescriptiopn); ...do stuff someTask.done();
 */
@AllArgsConstructor
@Log
public class Stopwatch {
  private final long startTime = System.nanoTime();
  private final String taskDescription;

  public void done() {
    log.info(
        () ->
            String.format(
                "%s took %s ms",
                taskDescription, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)));
  }
}
