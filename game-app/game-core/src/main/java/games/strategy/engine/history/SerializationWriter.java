package games.strategy.engine.history;

import java.io.Serializable;

/**
 * An object that is able to write a history node to a {@link HistoryWriter} for persistent storage.
 */
public interface SerializationWriter extends Serializable {
  void write(HistoryWriter writer);
}
