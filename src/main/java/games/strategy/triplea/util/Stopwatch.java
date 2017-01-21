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
  private final long m_startTime = System.currentTimeMillis();
  private final String m_taskDescription;
  private final Logger m_logger;
  private final Level m_level;

  public Stopwatch(final Logger logger, final Level level, final String taskDescription) {
    m_taskDescription = taskDescription;
    m_logger = logger;
    m_level = level;
  }

  public void done() {
    if (m_logger.isLoggable(m_level)) {
      m_logger.log(m_level, m_taskDescription + " took " + (System.currentTimeMillis() - m_startTime) + " ms");
    }
  }
}
