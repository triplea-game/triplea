package games.strategy.triplea.ui;

import java.util.logging.Level;

import lombok.extern.java.Log;

/**
 * When dealing with swing threads and new threads, exception handling can get tricky. Namely without
 * a handler to catch exceptions in these new threads, the stack traces will be poor. Specifically you
 * will get a stack trace that points to where you started the thread and not to the actual line within
 * the thread that had the problem.
 *
 * <p>
 * To solve this unhandled exception handlers get registered. For more details, see:
 * http://stackoverflow.com/questions/75218/how-can-i-detect-when-an-exceptions-been-thrown-globally-in-java#75439
 * </p>
 */
@Log
public class ErrorHandler implements Thread.UncaughtExceptionHandler {
  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    final String msg = "Error: " + (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e);
    log.log(Level.SEVERE, msg, e);
  }

  /**
   * Registers this class as an uncaught exception error handler.
   */
  public static void registerExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());
  }
}
