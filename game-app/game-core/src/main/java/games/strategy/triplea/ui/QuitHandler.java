package games.strategy.triplea.ui;

public interface QuitHandler {
  /**
   * Performs the quit operation, potentially prompting the user for confirmation.
   *
   * @return If false, the quit operation was canceled.
   */
  boolean shutdown();
}
