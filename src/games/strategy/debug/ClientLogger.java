package games.strategy.debug;

import java.io.PrintStream;

public class ClientLogger {
  private static final PrintStream developerOutputStream = System.out;
  private static final PrintStream userOutputStream = System.err;

  public static void logQuietly(final Throwable e) {
    log(developerOutputStream, e);
  }

  private static void log(final PrintStream stream, final Throwable t) {
    stream.println("Exception: " + t.getMessage());
    for (final StackTraceElement stackTraceElement : t.getStackTrace()) {
      stream.println(stackTraceElement.toString());
    }
  }

  public static void logQuietly(final String msg) {
    developerOutputStream.println(msg);
  }

  public static void logError(final Throwable t) {
    log(userOutputStream, t);
  }

  public static void logQuietly(final String msg, final Throwable t) {
    logQuietly(msg);
    logQuietly(t);
  }
}
