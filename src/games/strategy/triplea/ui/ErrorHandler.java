package games.strategy.triplea.ui;

import java.lang.Thread.UncaughtExceptionHandler;

import games.strategy.debug.ClientLogger;

public class ErrorHandler implements UncaughtExceptionHandler {

  public ErrorHandler() {}

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    String msgToLog = "Error on thread: " + thread.getName() + ", message: "
        + throwable.getMessage() + "; Diagnostic stack trace printed below.";
    ClientLogger.logQuietly(msgToLog);
    throwable.printStackTrace(System.out);
  }
}
