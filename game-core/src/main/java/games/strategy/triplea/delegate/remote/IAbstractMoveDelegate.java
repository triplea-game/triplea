package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import java.util.List;

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
@RemoteActionCode(4)
  List<T> getMovesMade();

  /**
   * Undoes the move at the specified index.
   *
   * @param moveIndex - an index in the list getMovesMade.
   * @return an error string if the move could not be undone, null otherwise
   */
@RemoteActionCode(12)
  String undoMove(int moveIndex);
}
