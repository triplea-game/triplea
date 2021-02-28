package games.strategy.engine.framework;

/** Holder for static 'game started' state. */
public enum GameState {
  ;

  private static boolean started = false;

  public static void setStarted() {
    started = true;
  }

  public static boolean notStarted() {
    return !started;
  }
}
