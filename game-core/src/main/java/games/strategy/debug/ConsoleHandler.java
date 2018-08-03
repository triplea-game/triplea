package games.strategy.debug;

import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * A {@link Handler} that publishes log records to an injected log handler. This can be wired up to send log messages
 * to a UI console.
 * <p>
 * Configuration: This handler does not currently support configuration through the {@link LogManager}.
 * It always uses the following default configuration:
 * </p>
 * <ul>
 * <li>Level: {@code Level.ALL}</li>
 * <li>Filter: No {@code Filter}</li>
 * <li>Formatter: {@code java.util.logging.SimpleFormatter}</li>
 * <li>Encoding: default platform encoding</li>
 * </ul>
 */
@Log
@AllArgsConstructor
public final class ConsoleHandler extends Handler {

  private final Consumer<LogRecord> msgReceiver;

  @Override
  public void close() {
    // no-op
  }

  @Override
  public void flush() {
    // no-op

  }

  @Override
  public void publish(final LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }
    msgReceiver.accept(record);
  }

  @Override
  public boolean isLoggable(final LogRecord record) {
    return (record != null) && super.isLoggable(record);
  }
}
