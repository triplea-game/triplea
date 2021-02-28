package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameState;
import games.strategy.engine.history.IDelegateHistoryWriter;

/**
 * Has a subset of the historyWriters functionality. Delegates should only have access to these
 * functions. The rest of the history writers functions should only be used by the GameData.
 */
public class GameDelegateHistoryWriter implements IDelegateHistoryWriter {
  private final IDelegateHistoryWriter delegateHistoryWriter;
  private final GameState gameData;

  GameDelegateHistoryWriter(
      final IDelegateHistoryWriter delegateHistoryWriter, final GameState data) {
    this.delegateHistoryWriter = delegateHistoryWriter;
    gameData = data;
  }

  private String getEventPrefix() {
    if (BaseEditDelegate.getEditMode(gameData.getProperties())) {
      return "EDIT: ";
    }
    return "";
  }

  @Override
  public void startEvent(final String eventName, final Object renderingData) {
    if (eventName.startsWith("COMMENT: ")) {
      delegateHistoryWriter.startEvent(eventName, renderingData);
    } else {
      delegateHistoryWriter.startEvent(getEventPrefix() + eventName, renderingData);
    }
  }

  @Override
  public void startEvent(final String eventName) {
    if (eventName.startsWith("COMMENT: ")) {
      delegateHistoryWriter.startEvent(eventName);
    } else {
      delegateHistoryWriter.startEvent(getEventPrefix() + eventName);
    }
  }

  @Override
  public void addChildToEvent(final String child) {
    if (child.startsWith("COMMENT: ")) {
      delegateHistoryWriter.addChildToEvent(child, null);
    } else {
      delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, null);
    }
  }

  @Override
  public void addChildToEvent(final String child, final Object renderingData) {
    if (child.startsWith("COMMENT: ")) {
      delegateHistoryWriter.addChildToEvent(child, renderingData);
    } else {
      delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, renderingData);
    }
  }
}
