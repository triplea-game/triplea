package games.strategy.debug;

import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.ui.SwingAction;

/**
 * A debug console window that displays the standard output and standard error streams.
 */
public final class ErrorConsole extends GenericConsole {
  private static final long serialVersionUID = -3489030525309243438L;
  private static ErrorConsole console;

  private ErrorConsole() {
    super("TripleA Console");
  }

  /**
   * Gets the singleton instance of the {@code ErrorConsole} class.
   *
   * @return An {@code ErrorConsole}.
   */
  public static ErrorConsole getConsole() {
    if (console == null) {
      SwingAction.invokeAndWait(() -> {
        console = new ErrorConsole();
        console.displayStandardOutput();
        console.displayStandardError();
        ErrorHandler.registerExceptionHandler();
      });
    }
    return console;
  }

  @Override
  public GenericConsole getConsoleInstance() {
    return getConsole();
  }
}
