package games.strategy.engine.chat;

import java.util.ArrayList;
import java.util.List;

class SentMessagesHistory {
  private final List<String> history = new ArrayList<>();
  private int historyPosition;

  SentMessagesHistory() {}

  public void next() {
    historyPosition = Math.min(historyPosition + 1, history.size());
  }

  public void prev() {
    historyPosition = Math.max(historyPosition - 1, 0);
  }

  public String current() {
    if (historyPosition == history.size()) {
      return "";
    }
    return history.get(historyPosition);
  }

  public void append(final String s) {
    history.add(s);
    historyPosition = history.size();
    if (history.size() > 100) {
      history.subList(0, 50).clear();
    }
  }
}
