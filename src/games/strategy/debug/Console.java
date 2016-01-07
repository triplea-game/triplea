package games.strategy.debug;

public class Console extends GenericConsole {
  private static final long serialVersionUID = -3489030525309243438L;
  private static Console s_console;

  public static Console getConsole() {
    if (s_console == null) {
      s_console = new Console();
    }
    return s_console;
  }

  /** Creates a new instance of Console */
  public Console() {
    super("An error has occured!");
  }
}
