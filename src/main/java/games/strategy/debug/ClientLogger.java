package games.strategy.debug;

import java.io.PrintStream;
import java.util.Collection;

public class ClientLogger {
  private static final PrintStream developerOutputStream = System.out;
  private static final PrintStream userOutputStream = System.err;

  public static void logQuietly(final Throwable e) {
    log(developerOutputStream, e);
  }

  private static void log(final PrintStream stream, final Throwable e) {
    e.printStackTrace(stream);
  }

  public static void logQuietly(final String msg) {
    developerOutputStream.println(msg);
  }

  public static void logQuietly(final String msg, final Throwable e) {
    logQuietly(msg);
    logQuietly(e);
  }

  public static void logError(final Throwable e) {
    log(userOutputStream, e);
  }

  public static void logError(final String msg) {
    userOutputStream.println(msg);
  }

  public static void logError(final String msg, final Throwable e) {
    logError(msg);
    logError(e);
  }

  /**
   * Logs the specified message and collection of errors to the user output stream.
   *
   * @param msg The error message; may be {@code null}.
   * @param throwables The collection of errors; must not be {@code null}.
   */
  public static void logError(final String msg, final Collection<? extends Throwable> throwables) {
    logError(msg);
    throwables.forEach(ClientLogger::logError);
  }
}
