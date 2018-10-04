package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.List;

class AbstractMoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -4072966724295569322L;

  Serializable superState;
  // add other variables here
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public List<UndoableMove> m_movesToUndo;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public MovePerformer m_tempMovePerformer;
}
