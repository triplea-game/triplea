package games.strategy.debug;

import java.io.PrintStream;
import java.util.Set;

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

  public static void logError(final String msg, final Set<Exception> exceptions) {
    logError(msg);
    for (final Exception e : exceptions) {
      logError(e);
    }
  }
}
