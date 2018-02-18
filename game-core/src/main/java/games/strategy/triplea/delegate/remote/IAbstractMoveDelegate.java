package games.strategy.triplea.delegate.remote;

import java.util.List;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;

/**
 * Remote interface for MoveDelegate and PlaceDelegate.
 *
 * @param <T> The type of the move (typically {@code UndoableMove} or {@code UndoablePlacement}).
 */
public interface IAbstractMoveDelegate<T> extends IRemote, IDelegate {
  /**
   * Get the moves already made.
   *
   * @return A list of moves already made.
   */
  List<T> getMovesMade();

  /**
   * @param moveIndex
   *        - an index in the list getMovesMade.
   * @return an error string if the move could not be undone, null otherwise
   */
  String undoMove(int moveIndex);
}
