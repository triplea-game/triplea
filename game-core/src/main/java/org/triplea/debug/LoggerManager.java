package org.triplea.debug;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Manages all application-specific loggers and provides convenience methods for configuring them.
 */
public final class LoggerManager {
  /**
   * Stores strong references to application-specific loggers so they aren't GCed after being
   * configured.
   */
  private static final ImmutableCollection<Logger> loggers =
      Stream.of("games.strategy", "org.triplea", "swinglib", "tools")
          .map(Logger::getLogger)
          .collect(ImmutableList.toImmutableList());

  private LoggerManager() {}

  public static void setLogLevel(final Level level) {
    checkNotNull(level);

    loggers.forEach(logger -> logger.setLevel(level));
  }
}
