package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.List;

class AbstractMoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -4072966724295569322L;

  Serializable superState;
  List<UndoableMove> movesToUndo;
  MovePerformer tempMovePerformer;
}
