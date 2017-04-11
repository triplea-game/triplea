package games.strategy.performance;

import games.strategy.debug.GenericConsole;

public class PerformanceConsole extends GenericConsole {
  private static final long serialVersionUID = -1249524819991242464L;

  private static PerformanceConsole consoleInstance = new PerformanceConsole();

  public static PerformanceConsole getInstance() {
    return consoleInstance;
  }

  @Override
  public PerformanceConsole getConsoleInstance() {
    return getInstance();
  }

  public PerformanceConsole() {
    super("Performance Log");
  }

}
