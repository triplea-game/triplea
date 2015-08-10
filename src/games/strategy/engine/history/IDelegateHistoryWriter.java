package games.strategy.engine.history;

/**
 * Has a subset of the historyWriters functionality.
 * Delegates should only have access to these functions.
 * The rest of the history writers functions should only
 * be used by the GameData
 */
public interface IDelegateHistoryWriter {
  public void startEvent(String eventName);

  public void startEvent(String eventName, Object renderingData);

  public void addChildToEvent(String child);

  public void addChildToEvent(String child, Object renderingData);

  // public void setRenderingData(Object renderingData);
}
