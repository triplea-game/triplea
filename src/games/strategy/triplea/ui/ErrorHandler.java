package games.strategy.triplea.ui;

import games.strategy.debug.ClientLogger;

/**
 * When dealing with swing threads and new threads, exception handling can get tricky. Namely without
 * a handler to catch exceptions in these new threads, the stack traces will be poor. Specifically you
 * will get a stack trace that points to where you started the thread and not to the actual line within
 * the thread that had the problem.
 *
 * To solve this unhandled exception handlers get registered. For more details, see:
 * http://stackoverflow.com/questions/75218/how-can-i-detect-when-an-exceptions-been-thrown-globally-in-java#75439
 */
public class ErrorHandler implements Thread.UncaughtExceptionHandler, ErrorHandlerAwtEvents {

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    handle(e);
  }

  /**
   * Method used to handle errors. Called auto-magically by sun property
   */
  @Override
  public void handle(Throwable throwable) {
    try {
      ClientLogger.logError(throwable);
    } catch (Throwable t) {
      try {
          // if client logger fails fall back to methods that may still work
        String msg = "Original error: " + throwable.getMessage() + ", next error while handling it: " + t.getMessage();
        System.err.println(msg);
        t.printStackTrace();
      } catch (Throwable fatal) {
        // Swallow this last error, if anything is thrown we can have an infinite loop of error handling.
      }
    }
  }

  /**
   * Registers this class as an uncaught exception error handler.
   */
  public static void registerExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());
    System.setProperty("sun.awt.exception.handler", ErrorHandler.class.getName());
  }
}
