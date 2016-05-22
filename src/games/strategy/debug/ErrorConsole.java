package games.strategy.debug;

import games.strategy.common.swing.SwingAction;
import games.strategy.triplea.ui.ErrorHandler;

import javax.swing.SwingUtilities;

public class ErrorConsole extends GenericConsole {
  private static final long serialVersionUID = -3489030525309243438L;
  private static ErrorConsole console;

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

  /** Creates a new instance of Console */
  public ErrorConsole() {
    super("An error has occured!");
  }
}
