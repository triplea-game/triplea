package games.performance;

import games.strategy.debug.GenericConsole;

public class PerformanceConsole extends GenericConsole {
  private static final long serialVersionUID = -1249524819991242464L;

  private static PerformanceConsole consoleInstance;

  @Override
  public PerformanceConsole getConsoleInstance() {
    if (consoleInstance == null) {
      consoleInstance = new PerformanceConsole();
    }
    return consoleInstance;
  }

  public PerformanceConsole() {
    super("Performance Log");
  }

}