package games.strategy.debug;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.GraphicsEnvironment;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

/**
 * Provides methods for the client to write log messages.
 * <p>
 * In general, the {@code logError()} methods will send their output to the user output
 * (standard error) stream, while the {@code logQuietly()} methods will send their output to the
 * developer output (standard output) stream.
 * </p>
 */
public final class ClientLogger {
  private static final PrintStream developerOutputStream = System.out;

  private static volatile boolean enableErrorPopup = true;

  private ClientLogger() {}

  private static void log(final PrintStream stream, final Throwable e) {
    e.printStackTrace(stream);
  }


  @VisibleForTesting
  public static void disableErrorPopupForTesting() {
    enableErrorPopup = false;
  }

  @VisibleForTesting
  public static void resetErrorPopupForTesting() {
    enableErrorPopup = true;
  }

  public static void logQuietly(final @Nullable String msg) {
    developerOutputStream.println(msg);
  }

  public static void logQuietly(final @Nullable String msg, final Throwable e) {
    logQuietly(msg);
    log(developerOutputStream, e);
  }

  /**
   * Logs an error message to console, shows a pop-up message to user with the same error message.
   *
   * @param msg Message to be displayed in the error pop-up, this will be shown to users. Error
   *        message should be as well written as possible with context of what error happened
   *        and what it means for the game (should the player restart? continue playing?).
   */
  public static void logError(final @Nullable String msg) {
    logQuietly(msg);
    showErrorMessage(msg);
  }

  /**
   * Logs an error message and stack trace to console, shows a pop-up message to user with
   * an error message.
   *
   * @param msg Message to be displayed in the error pop-up, this will be shown to users. Error
   *        message should be as well written as possible with context of what error happened
   *        and what it means for the game (should the player restart? continue playing?).
   */
  public static void logError(final @Nullable String msg, final Throwable e) {
    logQuietly(msg, e);
    showErrorMessage(msg);
  }

  /**
   * Overload of {@link #logError(String, Throwable)} specialized for {@code ExecutionException}. An
   * {@code ExecutionException} contains no useful information; it's simply an adapter to tunnel exceptions thrown by
   * tasks through the {@code Executor} API. We only log the cause to reduce the number of stack trace frames visible
   * to the user.
   */
  public static void logError(final String msg, final ExecutionException e) {
    checkNotNull(msg);
    checkNotNull(e);

    logError(msg, e.getCause());
  }

  private static void showErrorMessage(final String msg) {
    if (GraphicsEnvironment.isHeadless() || !enableErrorPopup) {
      // skip the pop-up if we there is no Swing UI to show the error message.
      // in all cases the error information should have been quiet logged, the error message pop-up is only
      // extra to show a clean error message to user, so we lose nothing by skipping it.
      return;
    }

    ErrorMessage.show(msg);
  }
}
