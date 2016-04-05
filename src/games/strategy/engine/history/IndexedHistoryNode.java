package games.strategy.engine.history;

public abstract class IndexedHistoryNode extends HistoryNode {
  private static final long serialVersionUID = 607716179473453685L;
  // points to the first change we are responsible for
  private final int m_changeStartIndex;
  // points after the last change we are responsible for
  private int m_changeStopIndex = -1;

  public IndexedHistoryNode(final String value, final int changeStartIndex) {
    super(value);
    m_changeStartIndex = changeStartIndex;
  }

  int getChangeStartIndex() {
    return m_changeStartIndex;
  }

  int getChangeEndIndex() {
    return m_changeStopIndex;
  }

  void setChangeEndIndex(final int index) {
    m_changeStopIndex = index;
  }
}
