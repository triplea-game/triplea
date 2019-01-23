package games.strategy.debug;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

/**
 * Manages all application-specific loggers and provides convenience methods for configuring them.
 */
final class LoggerManager {
  /**
   * Stores strong references to application-specific loggers so they aren't GCed after being configured.
   */
  private static final ImmutableCollection<Logger> loggers = getLoggers();

  private LoggerManager() {}

  private static ImmutableCollection<Logger> getLoggers() {
    return Stream.of("games.strategy", "org.triplea", "swinglib", "tools")
        .map(Logger::getLogger)
        .collect(ImmutableList.toImmutableList());
  }

  static void setLogLevel(final Level level) {
    checkNotNull(level);

    loggers.forEach(logger -> logger.setLevel(level));
  }
}
