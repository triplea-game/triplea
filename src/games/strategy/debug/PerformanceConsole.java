package games.strategy.debug;

public class PerformanceConsole extends GenericConsole {
  private static final long serialVersionUID = -1249524819991242464L;

  private static PerformanceConsole consoleInstance;

  public static PerformanceConsole getConsole() {
    if (consoleInstance == null) {
      consoleInstance = new PerformanceConsole();
    }
    return consoleInstance;
  }

  public PerformanceConsole() {
    super("Performance Log");
  }

}