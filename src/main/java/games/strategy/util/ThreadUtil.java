package games.strategy.util;

/** Utility class for java Thread related operations. */
public class ThreadUtil {

  /**
   * Sleeps the current thread, useful to handle interrupted exceptions.
   * This method sets the interrupted flag on the current thread, per best practice:
   *  - http://www.yegor256.com/2015/10/20/interrupted-exception.html
   *  - http://stackoverflow.com/questions/3976344/handling-interruptedexception-in-java
   *
   * @param millis Number of milliseconds to sleep
   * @return False on InterruptedException, true otherwise (implying that we have slept for the desired time duration)
   */
  public static boolean sleep(final int millis) {
    try {
      Thread.sleep(millis);
      return true;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
