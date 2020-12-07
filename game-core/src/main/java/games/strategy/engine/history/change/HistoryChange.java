package games.strategy.engine.history.change;

import games.strategy.engine.delegate.IDelegateBridge;

public interface HistoryChange {

  void perform(IDelegateBridge bridge);
}
