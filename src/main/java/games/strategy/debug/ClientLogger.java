package games.strategy.debug;

import java.io.PrintStream;
import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Provides methods for the client to write log messages.
 *
 * <p>
 * In general, the {@code logError()} methods will send their output to the user output (standard error) stream, while
 * the {@code logQuietly()} methods will send their output to the developer output (standard output) stream.
 * </p>
 */
public final class ClientLogger {
  private static final PrintStream developerOutputStream = System.out;
  private static final PrintStream userOutputStream = System.err;

  private ClientLogger() {}

  private static void log(final PrintStream stream, final Throwable e) {
    e.printStackTrace(stream);
  }

  public static void logQuietly(final Throwable e) {
    log(developerOutputStream, e);
  }

  public static void logQuietly(final @Nullable String msg) {
    developerOutputStream.println(msg);
  }

  public static void logQuietly(final @Nullable String msg, final Throwable e) {
    logQuietly(msg);
    logQuietly(e);
  }

  public static void logError(final Throwable e) {
    log(userOutputStream, e);
  }

  public static void logError(final @Nullable String msg) {
    userOutputStream.println(msg);
  }

  public static void logError(final @Nullable String msg, final Throwable e) {
    logError(msg);
    logError(e);
  }

  /**
   * Logs the specified message and collection of errors to the user output stream.
   *
   * @param msg The error message.
   * @param throwables The collection of errors.
   */
  public static void logError(final @Nullable String msg, final Collection<? extends Throwable> throwables) {
    logError(msg);
    throwables.forEach(ClientLogger::logError);
  }
}
