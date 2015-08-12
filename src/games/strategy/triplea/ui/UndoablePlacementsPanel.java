package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;

public class UndoablePlacementsPanel extends AbstractUndoableMovesPanel {
  private static final long serialVersionUID = -8905646288832196354L;

  public UndoablePlacementsPanel(final GameData data, final AbstractMovePanel movePanel) {
    super(data, movePanel);
  }

  protected final String getSpecificComponentForMoveLabel(final Route route) {
    return route.getStart().getName();
  }
}
