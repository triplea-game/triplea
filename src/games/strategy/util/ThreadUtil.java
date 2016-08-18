package games.strategy.util;

/** Utility class for java Thread related operations */
public class ThreadUtil {

  public static void sleep(final int millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      // ignore, general cause is the user sent an interrupt (killed the program)
    }
  }
}
