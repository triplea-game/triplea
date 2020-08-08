package org.triplea.debug;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A {@link Handler} that displays an error message dialog if any log record with a level equal to
 * or greater than {@link Level#WARNING} is published.
 *
 * <p><strong>Configuration:</strong> This handler does not currently support configuration through
 * the {@link LogManager}. It always uses the following default configuration:
 *
 * <ul>
 *   <li>Level: {@code Level.ALL}
 *   <li>Filter: No {@code Filter}
 *   <li>Formatter: No {@code Formatter}
 *   <li>Encoding: default platform encoding
 * </ul>
 */
public final class ErrorMessageHandler extends Handler {
  @VisibleForTesting static final int THRESHOLD_LEVEL_VALUE = Level.WARNING.intValue();

  @Override
  public void close() {}

  @Override
  public void flush() {}

  @Override
  public synchronized void publish(final LogRecord record) {
    publish(record, ErrorMessage::show);
  }

  @VisibleForTesting
  void publish(final LogRecord record, final Consumer<LogRecord> consumer) {
    if (isLoggable(record) && (record.getLevel().intValue() >= THRESHOLD_LEVEL_VALUE)) {
      consumer.accept(record);
    }
  }
}
