package games.strategy.debug;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * A {@link Handler} that publishes log records to an instance of {@link Console}.
 *
 * <p>
 * <strong>Configuration:</strong> This handler does not currently support configuration through the {@link LogManager}.
 * It always uses the following default configuration:
 * </p>
 * <ul>
 * <li>Level: {@code Level.ALL}</li>
 * <li>Filter: No {@code Filter}</li>
 * <li>Formatter: {@code java.util.logging.SimpleFormatter}</li>
 * <li>Encoding: default platform encoding</li>
 * </ul>
 */
public final class ConsoleHandler extends Handler {
  private final Console console;

  public ConsoleHandler(final Console console) {
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
