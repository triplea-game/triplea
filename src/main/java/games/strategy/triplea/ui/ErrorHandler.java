package games.strategy.triplea.ui;

import java.awt.GraphicsEnvironment;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.SystemProperties;

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
public class ErrorHandler implements Thread.UncaughtExceptionHandler, ErrorHandlerAwtEvents {

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    handle(e);
  }

  /**
   * Method used to handle errors. Called auto-magically by sun property
   */
  @Override
  public void handle(final Throwable throwable) {
    if (GraphicsEnvironment.isHeadless()) {
      final String msg = "Error: " + throwable.getMessage();
      System.err.println(msg);
      throwable.printStackTrace();
    } else {
      ClientLogger.logError("Error: " + throwable.getMessage(), throwable);
    }
  }

  /**
   * Registers this class as an uncaught exception error handler.
   */
  public static void registerExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new ErrorHandler());
    SystemProperties.setSunAwtExceptionHandler(ErrorHandler.class.getName());
  }
}
