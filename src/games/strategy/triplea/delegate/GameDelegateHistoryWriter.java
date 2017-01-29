package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.IDelegateHistoryWriter;

/**
 * Has a subset of the historyWriters functionality.
 * Delegates should only have access to these functions.
 * The rest of the history writers functions should only
 * be used by the GameData
 */
public class GameDelegateHistoryWriter implements IDelegateHistoryWriter {
  private IDelegateHistoryWriter m_delegateHistoryWriter;
  GameData m_data;

  GameDelegateHistoryWriter(final IDelegateHistoryWriter delegateHistoryWriter, final GameData data) {
    m_delegateHistoryWriter = delegateHistoryWriter;
    m_data = data;
  }

  private String getEventPrefix() {
    if (BaseEditDelegate.getEditMode(m_data)) {
      return "EDIT: ";
    }
    return "";
  }

  @Override
  public void startEvent(final String eventName, final Object renderingData) {
    if (eventName.startsWith("COMMENT: ")) {
      m_delegateHistoryWriter.startEvent(eventName, renderingData);
    } else {
      m_delegateHistoryWriter.startEvent(getEventPrefix() + eventName, renderingData);
    }
  }

  @Override
  public void startEvent(final String eventName) {
    if (eventName.startsWith("COMMENT: ")) {
      m_delegateHistoryWriter.startEvent(eventName);
    } else {
      m_delegateHistoryWriter.startEvent(getEventPrefix() + eventName);
    }
  }

  @Override
  public void addChildToEvent(final String child) {
    if (child.startsWith("COMMENT: ")) {
      m_delegateHistoryWriter.addChildToEvent(child, null);
    } else {
      m_delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, null);
    }
  }

  @Override
  public void addChildToEvent(final String child, final Object renderingData) {
    if (child.startsWith("COMMENT: ")) {
      m_delegateHistoryWriter.addChildToEvent(child, renderingData);
    } else {
      m_delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, renderingData);
    }
  }
}
