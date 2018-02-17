package games.strategy.engine.history;

public abstract class IndexedHistoryNode extends HistoryNode {
  private static final long serialVersionUID = 607716179473453685L;
  // points to the first change we are responsible for
  private final int changeStartIndex;
  // points after the last change we are responsible for
  private int changeStopIndex = -1;

  public IndexedHistoryNode(final String value, final int changeStartIndex) {
    super(value);
    this.changeStartIndex = changeStartIndex;
  }

  int getChangeStartIndex() {
    return changeStartIndex;
  }

  int getChangeEndIndex() {
    return changeStopIndex;
  }

  void setChangeEndIndex(final int index) {
    changeStopIndex = index;
  }
}
