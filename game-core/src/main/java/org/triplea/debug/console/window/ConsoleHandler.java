package org.triplea.debug.console.window;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A {@link Handler} that publishes log records to an instance of {@link ConsoleView}.
 *
 * <p><strong>Configuration:</strong> This handler does not currently support configuration through
 * the {@link LogManager}. It always uses the following default configuration:
 *
 * <ul>
 *   <li>Level: {@code Level.ALL}
 *   <li>Filter: No {@code Filter}
 *   <li>Formatter: {@code java.util.logging.SimpleFormatter}
 *   <li>Encoding: default platform encoding
 * </ul>
 */
public final class ConsoleHandler extends Handler {
  private final ConsoleView console;

  ConsoleHandler(final ConsoleView console) {
    checkNotNull(console);

    this.console = console;

    setFormatter(new SimpleFormatter());
  }

  @Override
  public void close() {}

  @Override
  public void flush() {}

  @Override
  public synchronized void publish(final LogRecord record) {
    if (isLoggable(record)) {
      console.append(getFormatter().format(record));
    }
  }
}
