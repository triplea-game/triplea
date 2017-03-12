package games.strategy.engine.history;

import java.io.Serializable;

public interface SerializationWriter extends Serializable {
  void write(HistoryWriter writer);
}
