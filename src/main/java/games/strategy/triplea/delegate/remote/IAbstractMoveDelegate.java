package games.strategy.triplea.delegate.remote;

import java.util.List;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;

/**
 * Remote interface for MoveDelegate and PlaceDelegate
 */
public interface IAbstractMoveDelegate extends IRemote, IDelegate {
  /**
   * Get the moves already made
   *
   * @return a list of UndoableMoves or UndoablePlacements
   */
  List<?> getMovesMade();


  /**
   * @param moveIndex
   *        - an index in the list getMovesMade
   * @return an error string if the move could not be undone, null otherwise
   */
  String undoMove(int moveIndex);
}
