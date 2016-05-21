package games.strategy.engine.chat;

import java.util.ArrayList;
import java.util.List;

class SentMessagesHistory {
  private final List<String> m_history = new ArrayList<String>();
  private int m_HistoryPosition;

  SentMessagesHistory() {}

  public void next() {
    m_HistoryPosition = Math.min(m_HistoryPosition + 1, m_history.size());
  }

  public void prev() {
    m_HistoryPosition = Math.max(m_HistoryPosition - 1, 0);
  }

  public String current() {
    if (m_HistoryPosition == m_history.size()) {
      return "";
    }
    return m_history.get(m_HistoryPosition);
  }

  public void append(final String s) {
    m_history.add(s);
    m_HistoryPosition = m_history.size();
    if (m_history.size() > 100) {
      m_history.subList(0, 50).clear();
    }
  }
}
