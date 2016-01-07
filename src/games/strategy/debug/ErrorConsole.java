package games.strategy.debug;

public class ErrorConsole extends GenericConsole {
  private static final long serialVersionUID = -3489030525309243438L;
  private static ErrorConsole s_console;

  public static ErrorConsole getConsole() {
    if (s_console == null) {
      s_console = new ErrorConsole();
    }
    return s_console;
  }

  /** Creates a new instance of Console */
  public ErrorConsole() {
    super("An error has occured!");
  }
}
