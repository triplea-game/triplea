package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;

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
  @Nullable
  String undoMove(int moveIndex);

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(10)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(11)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(9)
  @Override
  Serializable saveState();

  @RemoteActionCode(8)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
