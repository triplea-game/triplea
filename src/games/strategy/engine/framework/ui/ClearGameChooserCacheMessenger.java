package games.strategy.engine.framework.ui;

/**
 * A messenger between user actions and background map parsing threads. There are a number of
 * complications that arise because it can take some time to parse maps. If a game is started
 * before map parsing has completed then the results of map parsing are no longer needed.
 *
 * The game engine calls a "clear" cache method to clear out the map parsing cache when a game starts.
 * This messenger relays that "clear" message to the threads that are doing the background map parsing.
 */
public class ClearGameChooserCacheMessenger {
  private volatile boolean canceled = false;

  public ClearGameChooserCacheMessenger() {}

  /**
   * Call this method to indicate that no new map zip files are to be processed,
   * once the current map is processed, map 'refresh' parsing should return.
   */
  public void sendCancel() {
    canceled = true;
  }

  /**
   * Turn indicates population of the map cache has been cancelled and is no longer needed. False
   * indicates a game is not currently being played and background map parsing should proceed.
   */
  public boolean isCancelled() {
    return canceled;
  }
}
