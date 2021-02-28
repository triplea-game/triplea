package games.strategy.triplea.ui;

import games.strategy.triplea.delegate.AbstractUndoableMove;

class UndoablePlacementsPanel extends AbstractUndoableMovesPanel {
  private static final long serialVersionUID = -8905646288832196354L;

  UndoablePlacementsPanel(final AbstractMovePanel movePanel) {
    super(movePanel);
  }

  @Override
  protected void specificViewAction(final AbstractUndoableMove move) {}
}
