package games.strategy.debug;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import com.google.common.annotations.VisibleForTesting;

/**
 * A {@link Handler} that publishes log records of level {@link Level#WARNING} or {@link Level#SEVERE} to
 * {@code System.err} and log records of all other levels to {@code System.out}.
 *
 * <p>
 * Configuration: This handler does not currently support configuration through the {@link LogManager}.
 * It always uses the following default configuration:
 * </p>
 *
 * <ul>
 * <li>Level: {@code Level.ALL}</li>
 * <li>Filter: No {@code Filter}</li>
 * <li>Formatter: {@code java.util.logging.SimpleFormatter}</li>
 * <li>Encoding: default platform encoding</li>
 * </ul>
 */
public final class ConsoleHandler extends Handler {
  private final Supplier<PrintStream> errSupplier;
  private final Supplier<PrintStream> outSupplier;

  @SuppressWarnings("unused")
  public ConsoleHandler() {
    this(() -> System.out, () -> System.err);
  }

  @VisibleForTesting
  ConsoleHandler(final Supplier<PrintStream> outSupplier, final Supplier<PrintStream> errSupplier) {
    this.errSupplier = errSupplier;
    this.outSupplier = outSupplier;
    configure();
  }

  private void configure() {
    setLevel(Level.ALL);
    setFilter(null);
    setFormatter(new SimpleFormatter());
    try {
      setEncoding(null);
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError("default platform encoding should always be available", e);
    }
  }

  @Override
  public void close() {
    flush();
  }

  @Override
  public void flush() {
    errSupplier.get().flush();
    outSupplier.get().flush();
  }

  @Override
  public boolean isLoggable(final LogRecord record) {
    return (record != null) && super.isLoggable(record);
  }

  @Override
  public void publish(final LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }

    formatRecord(record).ifPresent(message -> writeMessage(record.getLevel(), message));
  }

  private Optional<String> formatRecord(final LogRecord record) {
    try {
      return Optional.of(getFormatter().format(record));
    } catch (final RuntimeException e) {
      reportError(null, e, ErrorManager.FORMAT_FAILURE);
      return Optional.empty();
    }
  }

  private void writeMessage(final Level level, final String message) {
    final PrintStream stream = getStreamFor(level);
    try {
      stream.print(message);
      stream.flush();
    } catch (final RuntimeException e) {
      reportError(null, e, ErrorManager.WRITE_FAILURE);
    }
  }

  private PrintStream getStreamFor(final Level level) {
    return (level.intValue() >= Level.WARNING.intValue()) ? errSupplier.get() : outSupplier.get();
  }
}
