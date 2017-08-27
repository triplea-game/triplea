package games.strategy.triplea.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for timing.
 * Use,
 * Stopwatch someTask = new StopWatch(someLogger, someLevel, taskDescriptiopn);
 * ...do stuff
 * someTask.done();
 */
public class Stopwatch {
  private final long startTime = System.currentTimeMillis();
  private final String taskDescription;
  private final Logger logger;
  private final Level level;

  public Stopwatch(final Logger logger, final Level level, final String taskDescription) {
    this.taskDescription = taskDescription;
    this.logger = logger;
    this.level = level;
  }

  public void done() {
    if (logger.isLoggable(level)) {
      logger.log(level, taskDescription + " took " + (System.currentTimeMillis() - startTime) + " ms");
    }
  }
}
