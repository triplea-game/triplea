package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.AbstractUndoableMove;
import games.strategy.triplea.delegate.UndoableMove;

public class UndoableMovesPanel extends AbstractUndoableMovesPanel {
  private static final long serialVersionUID = -3864287736715943608L;

  public UndoableMovesPanel(final GameData data, final AbstractMovePanel movePanel) {
    super(data, movePanel);
  }

  @Override
  protected final void specificViewAction(final AbstractUndoableMove move) {
    movePanel.getMap().setRoute(((UndoableMove) move).getRoute());
  }
}
