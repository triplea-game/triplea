package games.strategy.util;

/** Utility class for java Thread related operations */
public class ThreadUtil {

  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {

    }
  }
}
