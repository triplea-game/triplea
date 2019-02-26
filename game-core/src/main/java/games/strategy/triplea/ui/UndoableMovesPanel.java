package games.strategy.triplea.ui;

import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.delegate.UndoableMove;

class UndoableMovesPanel extends AbstractUndoableMovesPanel {
  private static final long serialVersionUID = -3864287736715943608L;

  UndoableMovesPanel(final AbstractMovePanel movePanel) {
    super(movePanel);
  }

  @Override
  protected final void specificViewAction(final AbstractUndoableMove move) {
    movePanel.getMap().setRoute(((UndoableMove) move).getRoute());
  }
}
