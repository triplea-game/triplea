package games.strategy.engine.history;

/**
 * Has a subset of the historyWriters functionality.
 * Delegates should only have access to these functions.
 * The rest of the history writers functions should only
 * be used by the GameData
 */
public interface IDelegateHistoryWriter {
  void startEvent(String eventName);

  void startEvent(String eventName, Object renderingData);

  void addChildToEvent(String child);

  void addChildToEvent(String child, Object renderingData);
  // public void setRenderingData(Object renderingData);
}
